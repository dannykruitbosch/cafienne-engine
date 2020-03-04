package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidMessageHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(InvalidMessageHandler.class);

    public InvalidMessageHandler(A actor, Object msg) {
        super(actor, msg);
    }

    @Override
    final protected InvalidCommandException runSecurityChecks() {
        return null;
    }

    protected void process() {

    }

    protected void complete() {
        if (msg instanceof ModelCommand) {
            // Sent to the wrong type of actor
            ModelCommand invalidCommand = (ModelCommand) msg;
            // Still set the actor, so that it can create a proper failure response.
            invalidCommand.setActor(this.actor);
            Exception wrongCommandType = new Exception("ModelActor of type '" + this.actor.getClass().getSimpleName() + "' does not support commands of type " + invalidCommand.getClass().getName());
            addDebugInfo(() -> wrongCommandType.getMessage(), logger);
            CommandFailure response = new CommandFailure((ModelCommand) msg, wrongCommandType);
            actor.reply(response);
        } else {
            logger.warn(actor.getClass().getSimpleName() + " " + actor.getId() + " received a message it cannot handle, of type " + msg.getClass().getName());
        }

        actor.persistEvents(events);
    }

    @Override
    protected TenantUser getUser() {
        return null;
    }
}
