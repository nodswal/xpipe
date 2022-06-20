package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.nio.file.Path;

@Value
@JsonTypeName("file")
public class FileStore implements StreamDataStore, FilenameStore {

    public static FileStore local(Path p) {
        return new FileStore(MachineStore.local(), p.toString());
    }

    public static FileStore local(String p) {
        return new FileStore(MachineStore.local(), p);
    }

    MachineStore machine;
    String file;

    @JsonCreator
    public FileStore(MachineStore machine, String file) {
        this.machine = machine;
        this.file = file;
    }

    @Override
    public boolean canOpen() {
        return machine.exists(file);
    }

    @Override
    public String toDisplay() {
        return file + "@" + machine.toDisplay();
    }

    @Override
    public boolean persistent() {
        return true;
    }

    @Override
    public String getFileName() {
        return file;
    }
}