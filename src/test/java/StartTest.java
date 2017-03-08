/**
 * Created by ravipatel on 3/2/17.
 */

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StartTest {

//    @Test
//    public void evaluatesExpression() {
//        Start start = new Start();
//        assertEquals("Hello World!!", start.printHello());
//    }

    @Test
    public void evaluateExpression1(){
        Start start = new Start();
        assertEquals("Hello World", start.printHello());
    }

}
