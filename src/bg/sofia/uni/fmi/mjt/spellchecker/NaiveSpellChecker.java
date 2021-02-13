package bg.sofia.uni.fmi.mjt.spellchecker;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class NaiveSpellChecker implements SpellChecker {
    private Set<String> words;
    private Set<String> stopWords;

    /**
     * Creates a new instance of NaiveSpellCheckTool, based on a dictionary of words and stop words
     *
     * @param dictionaryReader a java.io.Reader input stream containing list of words which will serve as a dictionary for the tool
     * @param stopwordsReader a java.io.Reader input stream containing list of stopwords
     */
    public NaiveSpellChecker(Reader dictionaryReader, Reader stopwordsReader){
        this.words = new TreeSet<>();
        this.stopWords = new TreeSet<>();

        try(BufferedReader dictBufferedReader = new BufferedReader(dictionaryReader);
            BufferedReader stopWordsBufferedReader = new BufferedReader(stopwordsReader)){
            String word = dictBufferedReader.readLine();
            while(word != null) {
                word = cleanUpWord(word);
                if(word.length() > 1) this.words.add(word);
                word = dictBufferedReader.readLine();
            }

            word = stopWordsBufferedReader.readLine();
            while (word != null) {
                word = cleanUpWord(word);
                this.stopWords.add(word);
                word = stopWordsBufferedReader.readLine();
            }

        } catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void analyze(Reader textReader, Writer output, int suggestionsCount) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader input = new BufferedReader(textReader)){
            int a = input.read();
            while(a != -1){
                output.write((char)a);
                sb.append((char)a);
                a = input.read();
            }
            output.write('\n');
        } catch (IOException e){
            throw new UncheckedIOException(e);
        }

        List<String> lines = Arrays.asList(sb.toString().split("\n"));

        Metadata metadata = metadata(new StringReader(sb.toString()));
        try (BufferedWriter writer = new BufferedWriter(output)){
            writer.write("= = = Metadata = = =");
            writer.write('\n');
            writer.write(String.format("%d characters, %d words, %d spelling issue(s) found\n", metadata.getCharacters(),
                    metadata.getWords(), metadata.getMistakes()));
            writer.write("= = = Findings = = =");
            writer.write('\n');
            for(int i = 0; i < lines.size(); i++){
                String line = lines.get(i);
                Set<String> misspelledWords = new HashSet<>(getWordList(line))
                        .stream().filter(x -> !this.words.contains(x)).collect(Collectors.toSet());
                for(String misspelledWord : misspelledWords){
                    List<String> suggestions = findClosestWords(misspelledWord, suggestionsCount);
                    writer.write(String.format("Line #%d, {%s} - Possible suggestions are {%s}",
                            i+1, misspelledWord, String.join(", ", suggestions)));
                    writer.write('\n');
                }
            }
        } catch (IOException e){
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public Metadata metadata(Reader textReader) {
        Metadata metadata = new Metadata();
        StringBuilder sb = new StringBuilder();
        int characters = 0;
        try {
            int c = textReader.read();
            while(c != -1){
                sb.append((char)c);
                if(!Character.isWhitespace((char)c)) characters++;
                c = textReader.read();
            }
        } catch (IOException e){
            throw new UncheckedIOException(e);
        }

        List<String> words = getWordList(sb.toString());

        int wordCount = words.size();

        int issues = (int)words.stream().filter(x -> !this.words.contains(x)).count();

        return metadata.record(characters, wordCount, issues);
    }

    @Override
    public List<String> findClosestWords(String word, int n) {
        Map<String, Double> scores = new HashMap<>();

        for(String candidateWord : this.words){
            double score = calculateCosineSimilarity(word, candidateWord);
            scores.put(candidateWord, score);
        }

        return scores.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder())).limit(n)
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private String cleanUpWord(String word){
        if(word == null) return null;
        return word.toLowerCase().replaceAll("[^a-zA-Z0-9]+", "").trim();
    }

    private List<String> getWordList(String str){
        return Arrays.stream(str.split("\\s+"))
                .map(this::cleanUpWord)
                .filter(x -> !this.stopWords.contains(x) && !x.isBlank())
                .collect(Collectors.toList());
    }

    private double calculateCosineSimilarity(String a, String b){
        Map<String, Integer> aVector = createVector(a);
        Map<String, Integer> bVector = createVector(b);

        double vectorProduct = 0;

        for(Map.Entry<String, Integer> entry : aVector.entrySet()){
            String gram = entry.getKey();
            if(bVector.containsKey(gram)){
                vectorProduct += entry.getValue() * bVector.get(gram);
            }
        }

        int aLength = getLengthOfVector(aVector);
        int bLength = getLengthOfVector(bVector);

        return vectorProduct/(double)(aLength * bLength);
    }

    private Map<String, Integer> createVector(String word){
        Map<String, Integer> vector = new HashMap<>();
        for(int i = 0; i < word.length() - 1; i++){
            String gram = "" + word.charAt(i) + word.charAt(i + 1);
            if(vector.containsKey(gram)){
                vector.replace(gram, vector.get(gram) + 1);
            } else {
                vector.put(gram, 1);
            }
        }
        return vector;
    }

    private int getLengthOfVector(Map<String, Integer> vector){
        try {
            return vector.values().stream()
                    .reduce((a, b) -> a*a + b*b).get();
        } catch (NoSuchElementException e){
            return 0;
        }
    }
}