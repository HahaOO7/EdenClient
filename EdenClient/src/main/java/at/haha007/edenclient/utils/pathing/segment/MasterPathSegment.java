package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MasterPathSegment extends PathSegment {
    private final List<PathSegment> children;

    public MasterPathSegment(List<PathSegment> children) {
        super(children.getFirst().from(), children.getLast().to());
        this.children = Collections.unmodifiableList(children);
    }

    @Override
    @NotNull
    public Task follower() {
        Task task = () -> {
        };
        for (PathSegment child : children) {
            task = task.then(child.follower());
        }
        return task;
    }

    public List<PathSegment> children() {
        return children;
    }
}
