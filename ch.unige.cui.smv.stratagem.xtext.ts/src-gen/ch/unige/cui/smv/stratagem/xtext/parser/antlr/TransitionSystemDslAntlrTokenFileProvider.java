/*
* generated by Xtext
*/
package ch.unige.cui.smv.stratagem.xtext.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class TransitionSystemDslAntlrTokenFileProvider implements IAntlrTokenFileProvider {
	
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
    	return classLoader.getResourceAsStream("ch/unige/cui/smv/stratagem/xtext/parser/antlr/internal/InternalTransitionSystemDsl.tokens");
	}
}
