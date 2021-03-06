package slp.core.lexing.simple;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import slp.core.lexing.Lexer;

public class TokenizedLexer implements Lexer {

	@Override
	public Stream<Stream<String>> lex(Stream<String> lines) {
		return lines.map(line -> Arrays.stream(line.split("\t")).filter(f -> !f.trim().isEmpty()));
	}
	
	@Override
	public Stream<Stream<String>> lex(List<String> lines) {
		return lex(lines.stream());
	}
}
