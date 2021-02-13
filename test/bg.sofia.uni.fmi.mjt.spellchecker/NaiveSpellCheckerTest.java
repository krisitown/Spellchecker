package bg.sofia.uni.fmi.mjt.spellchecker;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static org.junit.Assert.*;

public class NaiveSpellCheckerTest {
    private final String DICTIONARY = "src/resources/dictionary.txt";
    private final String STOPWORDS = "src/resources/stopwords.txt";

    private SpellChecker spellChecker;


    @Before
    public void createSpellChecker(){
        Reader dictionaryReader = null;
        Reader stopwordsReader = null;
        try {
            dictionaryReader = new FileReader(DICTIONARY);
        } catch (IOException e){
            e.printStackTrace();
            fail("Could not read dictionary file!");
        }

        try {
            stopwordsReader = new FileReader(STOPWORDS);
        } catch (IOException e){
            e.printStackTrace();
            fail("Could not read stopwords file!");
        }
        this.spellChecker = new NaiveSpellChecker(dictionaryReader, stopwordsReader);
    }

    @Test
    public void testWithEmptyTextShouldReturnEmptyMetadata(){
        Reader emptyStringReader = new StringReader("");
        Metadata metadata = this.spellChecker.metadata(emptyStringReader);
        assertEquals(metadata.getCharacters(), 0);
        assertEquals(metadata.getWords(), 0);
        assertEquals(metadata.getMistakes(), 0);
    }

    @Test
    public void testWithBlankTextShouldReturnEmptyMetadata(){
        Reader blankStringReader = new StringReader("\t   ");
        Metadata metadata = this.spellChecker.metadata(blankStringReader);
        assertEquals(metadata.getCharacters(), 0);
        assertEquals(metadata.getWords(), 0);
        assertEquals(metadata.getMistakes(), 0);
    }

    @Test
    public void testSingleWordMetadataWithNoMistakes(){
        Reader oneWordReader = new StringReader(" app ");
        Metadata metadata = this.spellChecker.metadata(oneWordReader);
        assertEquals(metadata.getCharacters(), 3);
        assertEquals(metadata.getWords(), 1);
        assertEquals(metadata.getMistakes(), 0);
    }

    @Test
    public void testSingleWordMetadataWithMistake(){
        Reader oneWordMisspelledReader = new StringReader(" aple ");
        Metadata metadata = this.spellChecker.metadata(oneWordMisspelledReader);
        assertEquals(metadata.getCharacters(), 4);
        assertEquals(metadata.getWords(), 1);
        assertEquals(metadata.getMistakes(), 1);
    }

    @Test
    public void testSimilarWordsLength(){
        String word = "apple";
        assertEquals(this.spellChecker.findClosestWords(word, 0).size(), 0);
        assertEquals(this.spellChecker.findClosestWords(word, 1).size(), 1);
        assertEquals(this.spellChecker.findClosestWords(word, 3).size(), 3);
    }

    @Test
    public void testSimilarWord(){
        String word = "appp";
        List<String> suggestions = this.spellChecker.findClosestWords(word, 1);
        assertEquals("app", suggestions.get(0));
    }

    @Test
    public void testAnalyzeSingleWordCorrect(){
        String text = "Apple. ";
        Writer testWriter = new StringWriter();
        Reader testReader = new StringReader(text);
        this.spellChecker.analyze(testReader, testWriter, 1);
        String expectedOutput = text + '\n'
                + "= = = Metadata = = =\n" +
                "6 characters, 1 words, 0 spelling issue(s) found\n" +
                "= = = Findings = = =\n";

        assertEquals(expectedOutput, testWriter.toString());
    }

    @Test
    public void testAnalyzeSingleWordMissplled(){
        String text = "Appp. ";
        Writer testWriter = new StringWriter();
        Reader testReader = new StringReader(text);
        this.spellChecker.analyze(testReader, testWriter, 1);
        String expectedOutput =  text + '\n'
                + "= = = Metadata = = =\n" +
                "5 characters, 1 words, 1 spelling issue(s) found\n" +
                "= = = Findings = = =\n" +
                "Line #1, {appp} - Possible suggestions are {app}\n";

        assertEquals(expectedOutput, testWriter.toString());
    }

    @Test
    public void testAnalyzeMultipleLineText(){
        String text = "Appp\n apple\n hii";
        Writer testWriter = new StringWriter();
        Reader testReader = new StringReader(text);

        this.spellChecker.analyze(testReader, testWriter, 1);
        String expectedOutput = text + '\n'
                + "= = = Metadata = = =\n" +
                "12 characters, 3 words, 2 spelling issue(s) found\n" +
                "= = = Findings = = =\n" +
                "Line #1, {appp} - Possible suggestions are {app}\n" +
        "Line #3, {hii} - Possible suggestions are {hi}\n";

        assertEquals(expectedOutput, testWriter.toString());
    }
}