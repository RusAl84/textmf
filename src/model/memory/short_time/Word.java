package model.memory.short_time;

import common.Compare;
import model.NoContentException;
import model.Settings;

import java.util.*;

/**
 * Class Word is class that store each word from Text
 * Common Text Mining processor works only with this class
 */
public class Word {

    private long mCount = 1;

    private List<String> mWordForms = new ArrayList();

    private List<Double> mCompareLevelList = new ArrayList();

    private static final String TAG = Word.class.getSimpleName();

    private final Compare mCompare = Compare.getInstance();

    private Map<Word, WordsConnection> mConnections = new HashMap();

    private Type mWordType = Type.SIMPL;

    public enum Type {
        OBJECT,
        SIMPL
    }
    
    public List<String> getWordForms() {
    	return mWordForms;
    }

    /**
     * Create and return copy of String that Word store
     * @return copy of baseWord
     */
    @Override
    public String toString(){
       return mWordForms.get(0);
    }

    /**
     *
     * using default String hashCode method for generating hash
     * @see String.hashCode()
     *
     * @return toString().hashCode()
     */
    @Override
    public int hashCode(){
        return toString().hashCode();
    }

    /**
     *
     * Calculating equals to String or Word in other cases retur false
     * using method from Compare class
     * @see Compare
     * @see Compare.distanceBetweenWords(Word, Word)
     * @see Compare.distanceBetweenWords(String, String)
     * @see Word.COMPARE_MAX_LEVEL
     *
     * @return true if words is equals
     */
    @Override
    public boolean equals(Object word){

        if (word == this)
            return true;

        if (word instanceof Word){

            if (word == this)
                return true;

            if (word.toString().equals(toString()))
                return true;

            return equals(word.toString());
        }

        if (word instanceof String){

            String wordString = (String) word;
            String originalString = wordString;
            wordString = Word.clearString(wordString);
            int lengthDifferens = Math.abs(toString().length() - wordString.length());
            int minSizeWord = toString().length() > wordString.length()
                    ? wordString.length()
                    : toString().length();
            double compareDelta = (double)lengthDifferens / (double)minSizeWord;
            if (compareDelta >= 1 - Settings.WORDS_COMPARE_MAX_LEVEL)
                return false;

            if (mWordForms.contains(wordString))
                return true;

            double distance = mCompare.distanceBetweenWords(toString(), wordString);
            if (distance > Settings.WORDS_COMPARE_MAX_LEVEL){

                if (!mCompareLevelList.contains(distance))
                    mCompareLevelList.add(distance);

                mWordType = getWordType(originalString);

                addNewWordForm(wordString);
                return true;
            }
        }

        return false;
    }

    public static String clearString(String strForCleaning){
        strForCleaning = strForCleaning.trim();
        StringBuilder sb = new StringBuilder(strForCleaning.length());
        for (Character ch : strForCleaning.toCharArray()){
            if (Character.isAlphabetic(ch)
                    || Character.isDigit(ch))
                sb.append(ch);
        }
        return sb.toString();
    }

    public Type getWordType() {
        return mWordType;
    }

    public long getCount(){
        return mCount;
    }

    public double getAbstractoinLevel(){

        if (mCompareLevelList.size() == 0)
            return 0;

        double abstractionLeve = 0;

        for (Double compareLevel : mCompareLevelList){
            abstractionLeve += (1 - compareLevel);
        }

        abstractionLeve /= mCompareLevelList.size();

        return abstractionLeve;
    }

    public Set<Word> getConnectedWords(){
        return mConnections.keySet();
    }

    WordsConnection getConnection(Word word){
        return mConnections.get(word);
    }

    void refreshConnection(Word toWord, WordsConnection wordConnection){
        mConnections.put(toWord, wordConnection);
    }

    WordsConnection addConnection(Word toWord, double distance){
        if (!mConnections.containsKey(toWord)){
            mConnections.put(toWord, new WordsConnection(distance));
        } else {
            WordsConnection connection = mConnections.get(toWord);
            connection.addDistance(distance);
            connection.increaseCount();
        }
        return mConnections.get(toWord);
    }

    void increaseCount(){
        mCount++;
    }

    Word(String word) throws NoContentException{

        if (word == null || word.length() < Settings.MINIMUM_WORD_LENGTH)
            throw new NoContentException(TAG + " Word(String word): word == null || word.length() < MINIMUM_WORD_LENGTH");

        setWordFromString(word);
    }

    private void setWordFromString(String word){

        mWordType = getWordType(word);
        if (mWordForms == null)
            mWordForms = new ArrayList<>();
        mWordForms.add(new String(Word.clearString(word)));
    }

    private void addNewWordForm(Word newWordForm){
        addNewWordForm(newWordForm.toString());
    }

    private void addNewWordForm(String newWordForm){
        if (newWordForm == null || newWordForm.length() < 2)
            return;

        newWordForm = Word.clearString(newWordForm);

        synchronized (mWordForms){
            if (mWordForms.contains(newWordForm))
                return;

            mWordForms.add(newWordForm);
        }
    }

    private Type getWordType(String str){

        str = str.trim();
        if (str.length() < 2)
            return Type.SIMPL;

        if (Character.isAlphabetic(str.charAt(0))
                && Character.isUpperCase(str.charAt(0))){
            return Type.OBJECT;
        }

        return Type.SIMPL;
    }

    class WordsConnection{

        private long mCount = 1;

        private double mDistance;

        public WordsConnection(double distance){
            mDistance = distance;
        }

        public long getCount(){
            return mCount;
        }

        public void increaseCount(){
            mCount++;
        }

        public double getDistance(){
            return mDistance + 1;
        }

        public void addDistance(double distance){
            mDistance = (mDistance + distance) / 2;
        }

    }

}
