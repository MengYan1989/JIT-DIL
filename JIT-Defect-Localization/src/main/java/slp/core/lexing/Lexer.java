package slp.core.lexing;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import slp.core.io.Reader;

public interface Lexer {
	default Stream<Stream<String>> lex(File file) {
		System.out.println(file.getPath());
		return lex(Reader.readLines(file));
	}
	
	default Stream<Stream<String>> lex(String text) {
		return lex(Arrays.stream(text.split("\n")));
	}

	default Stream<Stream<String>> lex(Stream<String> lines) {
		return lex(lines.collect(Collectors.toList()));
	}

	Stream<Stream<String>> lex(List<String> lines);
}
