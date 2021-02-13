package bg.sofia.uni.fmi.mjt.spellchecker;

public class Metadata {
    private int characters;
    private int words;
    private int mistakes;

    public Metadata record (int characters, int words, int mistakes) {
        this.characters = characters;
        this.words = words;
        this.mistakes = mistakes;
        return this;
    }

    public int getCharacters() {
        return characters;
    }

    public int getWords() {
        return words;
    }

    public int getMistakes() {
        return mistakes;
    }
}
