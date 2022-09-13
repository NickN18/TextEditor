import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.io.IOException;
import java.util.Arrays;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        LibC.Termios termios = new LibC.Termios();
        int returnCode = LibC.INSTANCE.tcgetattr(LibC.SYSTEM_OUT, termios);

        if(returnCode != 0)
        {
            System.err.println("There was a problem calling tcgetattr");
            System.exit(returnCode);
        }

        /**
         * Essentially here we are turning off the ECHO, ICANON,
         */
        termios.c_lflag &= ~(LibC.ECHO | LibC.ICANON | LibC.IEXTEN | LibC.ISIG);
        termios.c_iflag &= ~(LibC.IXON | LibC.ICRNL);
        termios.c_oflag &= ~(LibC.OPOST);




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


    //
    int ECHO = 10;
    int TCSAFLUSH = 2;
    int IXON = 2000;
    int ICRNL = 400;
    int IEXTEN = 100000;
    int OPOST = 1;
    int VMIN = 6;
    int VTIME = 5;
    int TIOCGWINSZ = 0x5413;

    //Loading the C standard library for POSIX systems
    LibC INSTANCE = Native.load("c", LibC.class);

    @Structure.FieldOrder(value = {"c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc"})
    class Termios extends Structure
    {
        public int c_iflag, c_oflag, c_cflag, c_lflag;

        public byte[] c_cc = new byte[19];

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
