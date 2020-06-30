import java.io.File;

/** A DTO used for transporting everything necessary to generate an overlay. */
public class FileArch {
    public final File background;
    public final File[] ores;

    public FileArch(File background, File ores) {
        this.background = background;
        this.ores = ores.listFiles();
    }
}