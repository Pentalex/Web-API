package valandur.webapi.command;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.Tristate;
import valandur.webapi.Constants;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandSource implements org.spongepowered.api.command.CommandSource {
    private String name = Constants.NAME;
    private int waitLines = 0;
    private Queue<String> lines = new ConcurrentLinkedQueue<>();
    private boolean hideInConsole = false;

    public static CommandSource instance = new CommandSource();

    public CommandSource() { }
    public CommandSource(String name, int waitLines, boolean hideInConsole) {
        this.name = name;
        this.waitLines = waitLines;
        this.hideInConsole = hideInConsole;
    }

    public List<String> getLines() {
        return new ArrayList(this.lines);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<org.spongepowered.api.command.CommandSource> getCommandSource() {
        return null;
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return null;
    }

    @Override
    public SubjectReference asSubjectReference() {
        return null;
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return false;
    }

    @Override
    public SubjectData getSubjectData() {
        return null;
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return null;
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return true;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        return Tristate.TRUE;
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, SubjectReference parent) {
        return false;
    }

    @Override
    public List<SubjectReference> getParents(Set<Context> contexts) {
        return null;
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key) {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "webapi";
    }

    @Override
    public Set<Context> getActiveContexts() {
        return null;
    }

    @Override
    public void sendMessage(Text message) {
        lines.add(message.toPlain());
        if (waitLines > 0 && lines.size() >= waitLines) {
            waitLines = 0;
            synchronized (this) {
                notify();
            }
        }
    }

    @Override
    public MessageChannel getMessageChannel() {
        if (hideInConsole)
            return MessageChannel.TO_NONE;
        return MessageChannel.TO_CONSOLE;
    }

    @Override
    public void setMessageChannel(MessageChannel channel) { }
}
