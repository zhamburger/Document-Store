import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.impl.StackImpl;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class StackImplTest {
    private StackImpl<Command> stack;
    private Command cmd1;
    private Command cmd2;

    @Before
    public void initVariables() throws URISyntaxException {
        this.stack = new StackImpl<Command>();
        //uri & cmd 1
        URI uri1 = new URI("http://www.test1.net");
        this.cmd1 = new Command(uri1, target ->{
            return target.equals(uri1);
        });
        //uri & cmd 2
        URI uri2 = new URI("http://www.test2.net");
        this.cmd2 = new Command(uri2, target ->{
            return target.equals(uri2);
        });
        this.stack.push(this.cmd1);
        this.stack.push(this.cmd2);
    }

    @Test
    public void pushAndPopTest(){
        Command pcmd = stack.pop();
        assertEquals("first pop should've returned second command",this.cmd2,pcmd);
        pcmd = stack.pop();
        assertEquals("second pop should've returned first command",this.cmd1,pcmd);
    }

    @Test
    public void peekTest(){
        Command pcmd = this.stack.peek();
        assertEquals("first peek should've returned second command",this.cmd2,pcmd);
        pcmd = this.stack.pop();
        assertEquals("first pop should've returned second command",this.cmd2,pcmd);

        pcmd = this.stack.peek();
        assertEquals("second peek should've returned first command",this.cmd1,pcmd);
        pcmd = this.stack.pop();
        assertEquals("second pop should've returned first command",this.cmd1,pcmd);
    }
    @Test
    public void sizeTest(){
        assertEquals("two commands should be on the stack",2,this.stack.size());
        this.stack.peek();
        assertEquals("peek should not have affected the size of the stack",2,this.stack.size());
        this.stack.pop();
        assertEquals("one command should be on the stack after one pop",1,this.stack.size());
        this.stack.peek();
        assertEquals("peek still should not have affected the size of the stack",1,this.stack.size());
        this.stack.pop();
        assertEquals("stack should be empty after 2 pops",0,this.stack.size());
    }
}