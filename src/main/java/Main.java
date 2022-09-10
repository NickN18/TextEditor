import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

public class Main
{
    public static void main(String[] args)
    {

    }
}

interface LibC extends Library
{
    int SYSTEM_OUT = 0;
    int ISIG = 1;
    int ICANON = 2;
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



    }

    int tcgetattr(int fd, Termios termios);

    int tcsetattr(int fd, int optionalActions, Termios termios);



}
