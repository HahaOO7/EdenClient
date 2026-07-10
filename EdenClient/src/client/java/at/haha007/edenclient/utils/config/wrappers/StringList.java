package at.haha007.edenclient.utils.config.wrappers;

import java.util.ArrayList;
import java.util.Arrays;

public class StringList extends ArrayList<String> {

    public StringList(String... strings) {
        super(strings.length);
        this.addAll(Arrays.asList(strings));
    }

    public StringList() {
        super();
    }
}
