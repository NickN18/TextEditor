import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.util.Arrays;

public class Viewer {

    private static LibraryC.Termios originalAttributes;
    private static int rows = 10;
    private static int columns = 10;

    public static void main(String[] args) throws IOException {
        // System.out.println("Hello World");
        /*System.out.println("\033[4;44;31mHello World\033[0mHello");
        System.out.println("\033[2J");
        System.out.println("\033[5H");*/

        enableRawMode();
        initEditor();

        while (true){
            refreshScreen();
            int key = readKey();
            handleKey(key);
        }

    }

    private static void initEditor() {
        LibraryC.Winsize windowSize = getWindowSize();
        columns = windowSize.ws_col;
        rows = windowSize.ws_row;
    }

    private static void refreshScreen() {
        StringBuilder builder = new StringBuilder();

        builder.append("\033[2J");
        builder.append("\033[H");

        for (int i = 0; i < rows - 1; i++) {
            builder.append("~\r\n");
        }

        String statusMessage = "Marco Code's Editor - v0.0.1";
        builder.append("\033[7m")
                .append(statusMessage)
                .append(" ".repeat(Math.max(0, columns - statusMessage.length())))
                .append("\033[0m");

        builder.append("\033[H");
        System.out.print(builder);
    }


    private static int readKey() throws IOException {
        return System.in.read();
    }

    private static void handleKey(int key) {
        if (key == 'q') {
            System.out.print("\033[2J");
            System.out.print("\033[H");
            LibraryC.INSTANCE.tcsetattr(LibraryC.SYSTEM_OUT_FD, LibraryC.TCSAFLUSH, originalAttributes);
            System.exit(0);
        }
    }



    private static void enableRawMode() {
        LibraryC.Termios termios = new LibraryC.Termios();
        int rc = LibraryC.INSTANCE.tcgetattr(LibraryC.SYSTEM_OUT_FD, termios);

        if (rc != 0) {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(rc);
        }

        originalAttributes = LibraryC.Termios.of(termios);

        termios.c_lflag &= ~(LibraryC.ECHO | LibraryC.ICANON | LibraryC.IEXTEN | LibraryC.ISIG);
        termios.c_iflag &= ~(LibraryC.IXON | LibraryC.ICRNL);
        termios.c_oflag &= ~(LibraryC.OPOST);

       /* termios.c_cc[LibraryC.VMIN] = 0;
        termios.c_cc[LibraryC.VTIME] = 1;*/

        LibraryC.INSTANCE.tcsetattr(LibraryC.SYSTEM_OUT_FD, LibraryC.TCSAFLUSH, termios);
    }

    private static LibraryC.Winsize getWindowSize() {
        final LibraryC.Winsize winsize = new LibraryC.Winsize();
        final int rc = LibraryC.INSTANCE.ioctl(LibraryC.SYSTEM_OUT_FD, LibraryC.TIOCGWINSZ, winsize);

        if (rc != 0) {
            System.err.println("ioctl failed with return code[={}]" + rc);
            System.exit(1);
        }

        return winsize;
    }

}

interface LibraryC extends Library {

    int SYSTEM_OUT_FD = 0;
    int ISIG = 1, ICANON = 2, ECHO = 10, TCSAFLUSH = 2,
            IXON = 2000, ICRNL = 400, IEXTEN = 100000, OPOST = 1, VMIN = 6, VTIME = 5, TIOCGWINSZ = 0x5413;

    // we're loading the C standard library for POSIX systems
    LibraryC INSTANCE = Native.load("c", LibraryC.class);

    @Structure.FieldOrder(value = {"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class Winsize extends Structure {
        public short ws_row, ws_col, ws_xpixel, ws_ypixel;
    }



    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure {
        public int c_iflag, c_oflag, c_cflag, c_lflag;

        public byte[] c_cc = new byte[19];

        public Termios() {
        }

        public static Termios of(Termios t) {
            Termios copy = new Termios();
            copy.c_iflag = t.c_iflag;
            copy.c_oflag = t.c_oflag;
            copy.c_cflag = t.c_cflag;
            copy.c_lflag = t.c_lflag;
            copy.c_cc = t.c_cc.clone();
            return copy;
        }

        @Override
        public String toString() {
            return "Termios{" +
                    "c_iflag=" + c_iflag +
                    ", c_oflag=" + c_oflag +
                    ", c_cflag=" + c_cflag +
                    ", c_lflag=" + c_lflag +
                    ", c_cc=" + Arrays.toString(c_cc) +
                    '}';
        }
    }


    int tcgetattr(int fd, Termios termios);

    int tcsetattr(int fd, int optional_actions,
                  Termios termios);

    int ioctl(int fd, int opt, Winsize winsize);

}