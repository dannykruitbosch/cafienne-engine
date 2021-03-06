/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.mail;

import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

public class Mail<D extends MailDefinition> extends SubProcess<D> {
    private final static Logger logger = LoggerFactory.getLogger(Mail.class);

    public Mail(ProcessTaskActor processTask, D definition) {
        super(processTask, definition);
//        logger.warn("\tSENDING MAIL\t" + processTask.getId() + "\n");
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    private Session mailSession;
    private Transport transport;

    private void connectMailServer() throws MessagingException {
        processTaskActor.addDebugInfo(() -> "Connecting to mail server");
        long now = System.currentTimeMillis();

        Properties mailServerProperties = definition.getMailProperties();
        String userName = mailServerProperties.get("authentication.user").toString();
        String password = mailServerProperties.get("authentication.password").toString();
        mailSession = Session.getInstance(mailServerProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });

        transport = mailSession.getTransport();
        transport.connect();
        long done = System.currentTimeMillis();
//        System.out.println("Connect took " + (done - now) + " milliseconds");
        processTaskActor.addDebugInfo(() -> "Connect to mail server took " + (done - now) + " milliseconds");
    }

    private void disconnectMailServer() throws MessagingException {
        long now = System.currentTimeMillis();
        transport.close();
        long done = System.currentTimeMillis();
        processTaskActor.addDebugInfo(() -> "Disconnecting from mail server took " + (done - now) + " milliseconds");
    }

    private ValueMap getParameters() {
        ValueMap input = processTaskActor.getMappedInputParameters();
        return definition.convert(input);
    }

    @Override
    public void start() {
        // Get the input parameters
        ValueMap input = getParameters();

        // Setup email message and recipients
        try {
            connectMailServer();

            // Create a mail session and message to fill.
            MimeMessage mailMessage = new MimeMessage(mailSession);

            // First validate the recipient list.
            try {
                mailMessage.setRecipients(Message.RecipientType.TO, getAddresses(input.get("to"), "To"));
                mailMessage.setRecipients(Message.RecipientType.CC, getAddresses(input.get("cc"), "Cc"));
                mailMessage.setRecipients(Message.RecipientType.BCC, getAddresses(input.get("bcc"), "Bcc"));
            } catch (InvalidMailException ime) {
                raiseFault("Failed to set recipients for mail message", ime.getCause());
                return;
            }

            // Validate that the mail has recipients
            if (mailMessage.getAllRecipients() == null || mailMessage.getAllRecipients().length == 0) {
                raiseFault("Mail message has no recipients", new IllegalArgumentException("Mail message has no recipients"));
                return;
            }

            // Fill subject
            String subject = input.raw("subject");
            mailMessage.setSubject(subject);
            processTaskActor.addDebugInfo(() -> "Subject: " + subject);

            mailMessage.addFrom(getAddresses(input.get("from"), "From"));
            InternetAddress[] replyTo = getAddresses(input.get("replyTo"), "Reply-To");
            mailMessage.setReplyTo(replyTo);

            // Fill body/attachments
            Multipart multipart = new MimeMultipart();

            // Set mail content / body
            String body = input.raw("body");
            processTaskActor.addDebugInfo(() -> "Body: " + body);
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/html");
            multipart.addBodyPart(messageBodyPart);

            // Add the attachments if any
            ValueList attachments = input.withArray("attachments");
            processTaskActor.addDebugInfo(() -> "Adding " + attachments.size() + " attachments");
            attachments.forEach(attachment -> {
                if (!attachment.isMap()) {
                    processTaskActor.addDebugInfo(() -> "Attachment must be a json object with 'content' (base64 coded) and optional 'fileName' and 'mimeType'; found json content of type  " + attachment.getClass().getSimpleName());
                    return;
                }
                String fileName = attachment.asMap().raw("fileName");
                String content = attachment.asMap().raw("content");
                String mimeType = attachment.asMap().raw("mimeType");
                if (mimeType == null || mimeType.isBlank()) mimeType = "application/octet-stream";
                if (fileName == null || fileName.isBlank()) fileName = "";
                if (content == null || content.isBlank()) {
                    processTaskActor.addDebugInfo(() -> "Attachment must be a json object with 'content' (base64 coded) and optional 'fileName' and 'mimeType'; skipping attachment, because 'content' is missing.");
                    return;
                }
                BodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new ByteArrayDataSource(Base64.getDecoder().decode(content), mimeType);
                try {
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(fileName);
                    multipart.addBodyPart(attachmentPart);
                    final String attachmentFileName = fileName;
                    processTaskActor.addDebugInfo(() -> "Added attachment '" + attachmentFileName + "' of length " + content.length() + " bytes");

                } catch (MessagingException e) {
                    throw new InvalidMailException("Cannot add attachment with file name '" + fileName + "'", e);
                }
            });

            ValueMap invite = input.with("invite");
            if (!invite.getValue().isEmpty()) {
                BodyPart attachmentPart = new MimeBodyPart();
                try {
                    if (! invite.has("required") && ! invite.has("optional")) {
                        invite.put("required", input.get("to"));
                        invite.put("optional", input.get("cc"));
                    }
                    if (!invite.has("meetingName")) invite.putRaw("meetingName", subject);
                    String content = new CalendarInvite(invite).invite;
                    String fileName = "invite.ics";
                    String mimeType = "text/calendar";
                    DataSource source = new ByteArrayDataSource(content.getBytes(StandardCharsets.UTF_8), mimeType);
                    attachmentPart.setDataHandler(new DataHandler(source));
                    attachmentPart.setFileName(fileName);
                    multipart.addBodyPart(attachmentPart);
                    processTaskActor.addDebugInfo(() -> "Added calendar invite");
                } catch (MessagingException e) {
                    throw new InvalidMailException("Cannot add the invite attachment", e);
                }
            }

            mailMessage.setContent(multipart);

            processTaskActor.addDebugInfo(() -> "Sending message to mail server");
            long now = System.currentTimeMillis();
            Address[] recipients = mailMessage.getAllRecipients();
            transport.sendMessage(mailMessage, recipients);
            long done = System.currentTimeMillis();
//            System.out.println("Completed sending email in " + (done - now) + " milliseconds");

            processTaskActor.addDebugInfo(() -> "Completed sending email in " + (done - now) + " milliseconds");
            disconnectMailServer();
        } catch (AddressException aex) {
            raiseFault("Invalid email address in from and/or replyTo", aex);
            return;
        } catch (MessagingException mex) {
            raiseFault("Failed to generate email message", mex);
            return;
        }

        // Set processTaskActor to completed
        raiseComplete();
    }

    @Override
    public void suspend() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void resume() {
    }

    /**
     * Convert the json structure to a list of addresses
     *
     * @param input
     * @return
     */
    private InternetAddress[] getAddresses(Value<?> input, String fieldType) {
        Collection<InternetAddress> list = new ArrayList<>();
        if (input.isMap()) {
            list.add(getAddress(input));
        } else if (input.isList()) {
            input.asList().forEach(value -> list.add(getAddress(value)));
        } else if (input.getValue() == null) {
            // do nothing, the field is not defined.
        } else if (input.getValue() instanceof String) {
            list.add(getAddress(input));
        } else {
            // Wrong type of input; ignore it.
        }
        processTaskActor.addDebugInfo(() -> "Field " + fieldType + ": '" + list.stream().map(a -> a.toString()).collect(Collectors.joining("; ")) + "'");
        return list.toArray(new InternetAddress[list.size()]);
    }

    /**
     * Convert the json structure to an address
     *
     * @param value
     * @return
     */
    private InternetAddress getAddress(Value value) throws InvalidMailException {
        String email = "";
        String name = "";

        if (value.isMap()) {
            email = value.asMap().raw("email");
            name = value.asMap().raw("name");
            if (email == null) email = "";
            if (name == null) name = "";
        } else if (value.isPrimitive() && value instanceof StringValue) {
            email = ((StringValue) value).getValue();
        } else {
            throw new InvalidMailAddressException("Cannot extract an email address from an object of type " + value.getClass().getSimpleName());
        }
        if (email == null || email.isBlank()) {
            throw new InvalidMailAddressException("Missing email address in object of type " + value.getClass().getSimpleName());
        }
        try {
            InternetAddress ia = new InternetAddress(email, name);
            return ia;
        } catch (UnsupportedEncodingException ex) {
            throw new InvalidMailAddressException("Invalid email address " + email + " " + ex.getMessage(), ex);
        }
    }
}
