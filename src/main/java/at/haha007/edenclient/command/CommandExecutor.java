package at.haha007.edenclient.command;

public interface CommandExecutor {
	void executeCommand(Command command, String label, String[] args);
}
