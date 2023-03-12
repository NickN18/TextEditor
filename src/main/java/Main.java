import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Main
{
    private static final int ARROW_UP = 1000, ARROW_DOWN = 1001;
    private static final int ARROW_LEFT = 1002, ARROW_RIGHT = 1003;
    private static final int HOME = 1004;
    private static final int END = 1005;
    private static final int PAGE_UP = 1006;
    private static final int PAGE_DOWN = 1007;
    private static final int DEL = 1008;
    private static LibC.Termios originalAttributes;
    private static int rows = 10;
    private static int columns = 10;

    private static int cursorX = 0, cursorY = 0;
    private static int offsetX = 0, offsetY = 0;
    private static List<String> content = List.of();

    public static void main(String[] args) throws IOException
    {
        openFile(args);
        enableRawMode();
        initializeEditor();

        while(true)
        {
            scrollScreen();
            refreshScreen();
            int key = readInput();
            handleInput(key);
        }
    }

    private static void scrollScreen()
    {
        if(cursorY >= rows + offsetY)
        {
            offsetY = cursorY - rows + 1;
        } else if(cursorY < offsetY)
        {
            offsetY = cursorY;
        }

        /**
         * ---------------------------------------
         * HORIZONTAL SCROLLING
         * ---------------------------------------
         */
        if(cursorX >= rows + offsetX)
        {
            offsetX = cursorX - columns + 1;
        } else if(cursorX < offsetX)
        {
            offsetX = cursorX;
        }
    }

    private static void openFile(String[] args)
    {
        if(args.length == 1)
        {
            String filename = args[0];
            Path path = Path.of(filename);

            if(Files.exists(path))
            {
                try(Stream<String> stream = Files.lines(path)) {
                    content = stream.toList();
                } catch (IOException e) {
                    //TODO: Display message in status bar
                }
            }
        }
    }

    private static void initializeEditor()
    {
        LibC.WindowSize windowSize = getWindowSize();
        rows = windowSize.ws_row - 1;
        columns = windowSize.ws_col;

    }

    private static void refreshScreen()
    {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\033[H");

        drawFileContent(stringBuilder);
        drawStatusBar(stringBuilder);
        drawCursor(stringBuilder);

        System.out.println(stringBuilder);
    }

    private static void drawStatusBar(StringBuilder stringBuilder)
    {
        String status = "Nick's Text Editor - v1.0.0 | X:" + cursorX + "Y:" + cursorY;

        stringBuilder.append("\033[7m")
                .append(status)
                .append(" ".repeat(Math.max(0, columns - status.length())))
                .append("\033[0m");
    }

    private static void drawFileContent(StringBuilder stringBuilder)
    {
        for(int i = 0; i < rows; i++)
        {
            int fileIndex = offsetY + i;

            if(fileIndex >= content.size())
            {
                stringBuilder.append("~");
            } else
            {
                String line = content.get(fileIndex);
                int lineToDraw = line.length() - offsetX;

                if(lineToDraw < 0) { lineToDraw = 0; }
                if(lineToDraw > columns) { lineToDraw = columns; }

                if(lineToDraw > 0 )
                {
                    stringBuilder.append(line, offsetX, offsetX + lineToDraw);
                }
            }
            stringBuilder.append("\033[K\r\n");
        }
    }

    private static void drawCursor(StringBuilder stringBuilder) { stringBuilder.append(String.format("\033[%d;%dH", cursorY - offsetY + 1, cursorX - offsetX + 1)); }


    private static int readInput() throws IOException
    {
        int key = System.in.read();
        if(key != '\033') { return key; }

        int nextKey = System.in.read();
        if(nextKey != '[' && nextKey != 'O') { return nextKey; }

        int nextNextKey = System.in.read();

        if(nextKey == '[')
        {
            return switch (nextNextKey) {
                        case 'A' -> ARROW_UP;
                        case 'B' -> ARROW_DOWN;
                        case 'C' -> ARROW_RIGHT;
                        case 'D' -> ARROW_LEFT;
                        case 'H' -> HOME;
                        case 'F' -> END;

                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            int newChar = System.in.read();

                            if(newChar != '~') { yield newChar; }

                            switch (nextNextKey)
                            {
                                case '1':
                                case '7':
                                    yield HOME;

                                case '3':
                                    yield DEL;

                                case '4':
                                case '8':
                                    yield END;

                                case '5':
                                    yield PAGE_UP;

                                case '6':
                                    yield PAGE_DOWN;

                                default: yield nextNextKey;

                            }
                        }
                        default -> nextNextKey;
            };
        } else
        {
            return switch (nextNextKey) {
                        case 'H' -> HOME;
                        case 'F' -> END;
                        default -> nextNextKey;
            };
        }
    }

    private static void handleInput(int key)
    {
        if(key == 'q')
        {
            exit();
        } else if(List.of(ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT, HOME, END, PAGE_UP, PAGE_DOWN).contains(key))
        {
            moveCursor(key);
        }

    }

    private static void moveCursor(int key)
    {
        switch(key)
        {
            case ARROW_UP -> {
                if(cursorY > 0) { cursorY--; }
            }
            case ARROW_DOWN -> {
                if(cursorY < content.size()) { cursorY++; }
            }
            case ARROW_LEFT -> {
                if(cursorX > 0) { cursorX--; }
            }
            case ARROW_RIGHT -> {
                if(cursorX < content.get(cursorY).length() - 1) { cursorX++; }
            }
            case PAGE_UP, PAGE_DOWN ->  {
                if(key == PAGE_UP)
                {
                    moveToTopOfScreen();
                } else if(key == PAGE_DOWN)
                {
                    moveToBottomOfScreen();
                }

                for(int i = 0; i < rows; i++)
                {
                    moveCursor(key == PAGE_UP ? ARROW_UP : ARROW_DOWN);
                }

            }

            case HOME -> cursorX = 0;
            case END -> cursorX = columns - 1;

        }
    }

    private static void moveToTopOfScreen() { cursorY = offsetY; }

    private static void moveToBottomOfScreen()
    {
        cursorY = offsetY + rows - 1;
        if(cursorY > content.size()) { cursorY = content.size(); }
    }



    private static void exit()
    {
        System.out.println("\033[2J");
        System.out.println("\033[H");

        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT, LibC.TCSAFLUSH, originalAttributes);
        System.exit(0);
    }

    private static void enableRawMode()
    {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT, termios);

        if(returnCode != 0)
        {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(returnCode);
        }

        originalAttributes = LibC.Termios.of(termios);

        /**
         * Essentially here we are turning off the ECHO, ICANON, IEXTEN, ISIG flags
         */
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

        //termios.c_cc[LibC.VMIN] = 0;
        //termios.c_cc[LibC.VTIME] = 1;

        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT, LibC.TCSAFLUSH, termios);

    }
    private static LibC.WindowSize getWindowSize()
    {
        final LibC.WindowSize windowSize = new LibC.WindowSize();
        final int returnCode = LibC.INSTANCE.ioctl(LibC.SYSTEM_OUT, LibC.TIOCGWINSZ, windowSize);

        if(returnCode != 0)
        {
            System.out.println("ioctl failed with return code [={}]" + returnCode);
            System.exit(1);
        }
        return windowSize;
    }
}

interface LibC extends Library
{
    int SYSTEM_OUT = 0;

    //Whenever any of the characters "INTR", "QUIT", "SUSP", or "DSUSP" are received,
    //generate the corresponding signal
    int ISIG = 1;

    //Enables canonical mode
    int ICANON = 2;

    //Echo input characters
    int ECHO = 10;

    //This occurs after all output written to the object referred by fd has been transmitted
    //and all input that has been received but not read will be discarded before the change is made
    int TCSAFLUSH = 2;

    //Enables XON/XOFF flow control on output
    int IXON = 2000;

    //Translates carriage return to newline on input
    int ICRNL = 400;

    //Enables implementation-defined input processing
    int IEXTEN = 100000;

    //Enable implementation-defined output processing
    int OPOST = 1;

    //Minimum number of characters for non-canonical read
    int VMIN = 6;

    //Timeout in deciseconds for non-canonical read
    int VTIME = 5;

    //Gets what you need, in this case it means to get the window size
    int TIOCGWINSZ = 0x40087468;

    //Loading the C standard library for POSIX systems
    LibC INSTANCE = Native.load("c", LibC.class);

    @Structure.FieldOrder(value = {"ws_row", "ws_col", "ws_xpixel", "ws_ypixel"})
    class WindowSize extends Structure
    {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;

    }


    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure
    {
        public long c_iflag; // Input modes
        public long c_oflag; // Output modes
        public long c_cflag; // Control modes
        public long c_lflag; // Local modes

        public byte[] c_cc = new byte[19]; // Special characters

        public Termios() {}

        public static Termios of(Termios t)
        {
            Termios copy = new Termios();
            copy.c_iflag = t.c_iflag;
            copy.c_oflag = t.c_oflag;
            copy.c_cflag = t.c_cflag;
            copy.c_lflag = t.c_lflag;
            copy.c_cc = t.c_cc.clone();

            return copy;
        }

        @Override
        public String toString()
        {
            return "Termios {" +
                    "c_iflag = " + c_iflag +
                    ", c_oflag = " + c_oflag +
                    ", c_cflag = " + c_cflag +
                    ", c_lflag = " + c_lflag +
                    ", c_cc = " + Arrays.toString(c_cc) +
                    "}";
        }

    }

    int tcgetattr(int fd, Termios termios);

    int tcsetattr(int fd, int optionalActions, Termios termios);

    int ioctl(int fd, int opt, WindowSize windowSize);
}
