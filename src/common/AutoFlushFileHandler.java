package common;

import java.io.IOException;
import java.util.logging.*;

public class AutoFlushFileHandler extends FileHandler {
    public AutoFlushFileHandler(String pattern, boolean append) throws IOException, SecurityException {
        super(pattern, append);
    }
    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush(); // <-- force write to disk per record
    }
}
