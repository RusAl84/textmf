package model.memory.short_time;

import common.InputOpenException;
import common.ThemeSearcher;
import model.NoContentException;
import model.Settings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class Text {

    private static final String TAG = Text.class.getSimpleName();

    private final List<Sentanse> mTextSentanses = new CopyOnWriteArrayList<Sentanse>();

    private static final String REGEXP_EPLITTER = "[\\\"\\.!?\\[\\];\\(\\)][ ]";

    private String mLastString = "";

    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(Settings.MAX_THREADS);

    private final List<Future<?>> mTasks = new LinkedList<Future<?>>();

    private WordsStorage mWordsStorage = new WordsStorage();

    private List<Word> mWordsInText = null;

    private List<Word> mAllObjects = null;

    private double mMaxConnectionSize = Text.VALUE_NOT_SET;

    private final static int VALUE_NOT_SET = -1;

    public Text(String textFilePath) throws InputOpenException {

        try {
            parsInputText(new BufferedReader(new FileReader(textFilePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new InputOpenException(TAG+" Text(String textFilePath)", e);
        }

    }

    private void parsInputText(BufferedReader inputText) throws InputOpenException {
        try (Scanner inputScanner = new Scanner(inputText)){
            inputScanner.useDelimiter(Text.REGEXP_EPLITTER);
            String sentanse;

            while (inputScanner.hasNext()){
                sentanse = inputScanner.next();
                mTasks.add(mExecutorService.submit(new StringParsingTask(sentanse)));
            }
        } finally {
            mExecutorService.shutdown();
            while (!isTasksFinished()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isTasksFinished(){

        for (int i=0;i<mTasks.size();i++)
            if (mTasks.get(i).isDone())
                mTasks.remove(i);

        if (mTasks.size() == 0)
            return true;

        return false;
    }

    public int getSentansesCount(){
        return mTextSentanses.size();
    }

    private class StringParsingTask implements Runnable{

        private String mStringToParse;

        private StringParsingTask(String string){
            mStringToParse = new String(string);
        }

        @Override
        public void run() {
            try {
                mTextSentanses.add(Sentanse.generateSentanse(mStringToParse, mWordsStorage));
            } catch (NoContentException e) {
                e.printStackTrace();
            }
        }
    }

    public double getConnectionLevelBetweenWords(String wordStr1, String wordStr2) throws NoContentException {
        Word word1 = mWordsStorage.getWordFromString(wordStr1);
        Word word2 = mWordsStorage.getWordFromString(wordStr2);
        return getConnectionLevelBetweenWords(word1, word2);
    }

    public double getConnectionLevelBetweenWords(Word word1, Word word2){
        Word.WordsConnection wordsConnection = word1.getConnection(word2);
        long usageCount = wordsConnection.getCount();
        double exp = (double)usageCount / (double)(word1.getCount() + word2.getCount() - usageCount);
        exp = Math.exp(exp);
        double a = (getWordWeightWithoutConnectionCount(word1) + getWordWeightWithoutConnectionCount(word2))
                / (wordsConnection.getDistance()*2);
        return - ((a * exp) * (Math.log(a * exp) / Math.log(2)));
    }

    public double getWordWeight(String wordStr) throws NoContentException {
        Word word = mWordsStorage.getWordFromString(wordStr);
        return getWordWeight(word);
    }

    public double getWordWeight(Word word){

        double wordWeight = getWordWeightWithoutConnectionCount(word);

        if (Settings.sWeightCalculationMode == Settings.WeightCalculationMode.WITHOUT_CONNECTION_COUNT){
            return wordWeight;
        }

        return wordWeight *
                (1.1 - ((double)word.getConnectedWords().size() / (double)mWordsStorage.getMaxConnectionCount())) *
                (getWordMaximumConnection(word)/getMaxConnectionSize());
    }

    private double getWordWeightWithoutConnectionCount(Word word){
        long mostPopularWordCount = mWordsStorage.getMostPopularWord().getCount();
        long wordCount = word.getCount();
        double wordCountToMostPopularCount = (double)wordCount/(double)mostPopularWordCount;

        double wordWeight = -(wordCountToMostPopularCount *
                (Math.log(wordCountToMostPopularCount)/Math.log(2)));
        double abstractionLevel = (word.getAbstractoinLevel() + 0.1);
        abstractionLevel *= (double)word.getWordForms().size() / (double)mWordsStorage.getMaximumWordFormsCount();

        wordWeight *= abstractionLevel;
        return wordWeight;
    }

    public List<Word> getWords(){

        if (mWordsInText != null)
            return mWordsInText;

        List<Word> words = new ArrayList<>(mWordsStorage.getAllWord());
        Collections.sort(words, new WordCompratorByWeight());
        mWordsInText = words;
        return words;
    }

    public List<Word> getObjects(){

        if (mAllObjects != null)
            return mAllObjects;

        List<Word> allWordsInText = getWords();
        List<Word> allObjectsInText = new ArrayList<>();

        for (Word word : allWordsInText){
            if (word.getWordType() == Word.Type.OBJECT)
                allObjectsInText.add(word);
        }

        mAllObjects = allObjectsInText;
        return allObjectsInText;
    }

    public class WordCompratorByWeight implements Comparator<Word>{

        @Override
        public int compare(Word word1, Word word2) {
            double word1weight = getWordWeight(word1);
            double word2weight = getWordWeight(word2);
            double deltaWeight = word2weight - word1weight;

            if (deltaWeight == 0)
                return 0;

            while (deltaWeight < 1
                    && deltaWeight > -1)
                deltaWeight *= 10;
            return (int)(deltaWeight);
        }

    }

    public List<Word> getThem(){
        ThemeSearcher themeSearcher = null;
        try {
            themeSearcher = new ThemeSearcher(this);
        } catch (NoContentException e) {
            e.printStackTrace();
            return getWords();
        }
        return themeSearcher.getThem();
    }

    private double getMaxConnectionSize(){

        if (mMaxConnectionSize != Text.VALUE_NOT_SET)
            return mMaxConnectionSize;

        double maxConnectionLevel = 0;
        for (Word word : mWordsStorage.getAllWord()){

             double connectionLevel = getWordMaximumConnection(word);
            if (connectionLevel > maxConnectionLevel)
                maxConnectionLevel = connectionLevel;

        }
        mMaxConnectionSize = maxConnectionLevel;
        return maxConnectionLevel;
    }

    private double getWordMaximumConnection(Word word){

        double maxConnectionLevel = 0;
        for (Word connectedWord : word.getConnectedWords()){
            double connectionLevel = getConnectionLevelBetweenWords(word, connectedWord);
            if (connectionLevel > maxConnectionLevel)
                maxConnectionLevel = connectionLevel;
        }

        return maxConnectionLevel;
    }

}
