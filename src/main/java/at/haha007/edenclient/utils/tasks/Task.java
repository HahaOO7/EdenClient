package at.haha007.edenclient.utils.tasks;

public interface Task {
    //return true if the task is finished
    void run() throws InterruptedException;
}
