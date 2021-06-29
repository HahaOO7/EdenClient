package at.haha007.edenclient.command;

public class Command {
    private CommandExecutor executor;

    public Command(CommandExecutor executor) {
        this.executor = executor;
    }


    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }
}
