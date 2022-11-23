import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;


import java.io.IOException;
import java.util.Arrays;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        enableRawMode();

        while(true)
        {
            int key = System.in.read();

            if(key == 'q') { System.exit(0); }

            System.out.println((char) key +  " (" + key + ")\r\n");
        }
    }

    private static void enableRawMode() {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT, termios);

        if(returnCode != 0)
        {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(returnCode);
        }

        System.out.println("termios = " + termios);

        /**
         * Essentially here we are turning off the ECHO, ICANON,
         */
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);

        //termios.c_cc[LibC.VMIN] = 0;
        //termios.c_cc[LibC.VTIME] = 1;

        LibC.INSTANCE.tcsetattr(LibC.SYSTEM_OUT, LibC.TCSAFLUSH, termios);
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

    //Minimum number of characters for noncanonical read
    int VMIN = 6;

    //Timeout in deciseconds for noncanonical read
    int VTIME = 5;

    //Gets what you need, in this case it means to get the window size
    int TIOCGWINSZ = 0x40087468;

    //Loading the C standard library for POSIX systems
    LibC INSTANCE = Native.load("c", LibC.class);

    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure
    {
        public int c_iflag; // Input modes
        public int c_oflag; // Output modes
        public int c_cflag; // Control modes
        public int c_lflag; // Local modes

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

}
