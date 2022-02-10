package common;


import model.*;
import model.Settings;
import model.memory.short_time.Text;
import model.memory.short_time.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThemeSearcher {

    private static final String TAG = ThemeSearcher.class.getSimpleName();

    private final Word mStartingPoint;

    private final Word mEndPoint;

    private final Text mText;

    private final List<Word> mWordsThatWasVisited = new CopyOnWriteArrayList();

    private final ExecutorService mExecutorService = Executors.newFixedThreadPool(Settings.MAX_THREADS);

    private final List<Future<?>> mTasks = new CopyOnWriteArrayList();

    private final List<List<Word>> mThemes = new CopyOnWriteArrayList();

    private static final int NO_MAX_LEVEL = -1;

    public ThemeSearcher(Word startingPoint, Word endPoint, Text text){
        mStartingPoint = startingPoint;
        mEndPoint = endPoint;
        mText = text;
    }

    public ThemeSearcher(Text text) throws NoContentException {
        List<Word> words = text.getWords();

        if (words.size() < 2)
            throw new NoContentException(TAG + " ThemeSearcher(Text text): words.size() < 2");

        mStartingPoint = words.get(0);
        mEndPoint = getSecondWord(words);
        mText = text;
    }

    public List<Word> getThem(){
        List<Word> startingPath = new ArrayList();
        startingPath.add(mStartingPoint);
        new TextWalker(startingPath).run();
        while (!isTasksFinished()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return getBestTheme();
    }

    private Word getSecondWord(List<Word> words){
        Word firstWord = words.get(0);
        for (Word word : words){
            if (word == firstWord)
                continue;

            if (!word.getConnectedWords().contains(firstWord))
                return word;
        }
        return words.get(1);
    }

    private List<Word> getBestTheme(){

        if (mThemes.size() == 0)
            return new ArrayList();


        List<Word> bestThem = mThemes.get(0);
        double bestThemWeight = Double.MIN_VALUE;
        for (List<Word> them : mThemes){
            double themWeight = calculateThemWeight(them);
            if (themWeight> bestThemWeight){
                bestThemWeight = themWeight;
                bestThem = them;
            }
        }
        return bestThem;
    }

    private double calculateThemWeight(List<Word> them){

        double themWeight = 0;

        for (int i = 0; i< them.size() - 1;i++){
            Word currentWord = them.get(i);
            Word nextWord = them.get(i+1);
            double  connection = mText.getConnectionLevelBetweenWords(currentWord, nextWord);
            connection *= mText.getWordWeight(nextWord);
            connection /= 2;
            themWeight += connection;

        }

        return themWeight / (them.size() - 1);

    }

    private boolean isTasksFinished(){

        for (int i=0;i<mTasks.size();i++)
            if (mTasks.get(i).isDone())
                mTasks.remove(i--);

        if (mTasks.size() == 0)
            return true;

        return false;
    }

    class TextWalker implements Runnable{

        private List<Word> mWordsPath;

        private Word mLastWordInPath;

        private List<Word> mNextWords;

        TextWalker(List<Word> wordsPath){
            mWordsPath = wordsPath;
            mLastWordInPath = wordsPath.get(wordsPath.size() - 1);
            mNextWords = nextWords();
            if (!mWordsThatWasVisited.contains(mLastWordInPath))
                mWordsThatWasVisited.add(mLastWordInPath);
        }

        private double getMaximumConnectionLevel(){
            double maxLevel = ThemeSearcher.NO_MAX_LEVEL;

            Set<Word> nextWords = mLastWordInPath.getConnectedWords();
            for (Word word : nextWords){
                if (!mWordsThatWasVisited.contains(word)){
                    double conectionLevel = mText.getConnectionLevelBetweenWords(mLastWordInPath, word);
                    if (conectionLevel > maxLevel)
                        maxLevel = conectionLevel;
                }
            }

            return maxLevel;

        }

        private List<Word> nextWords(){

            double maxConnectionLevel = getMaximumConnectionLevel();
            if (maxConnectionLevel == ThemeSearcher.NO_MAX_LEVEL)
                return new ArrayList();

            maxConnectionLevel *= 1 - Settings.DEEP_LEVEL_THEME_SEARCH;

            List<Word> nextWords = new ArrayList();
            Set<Word> connectedWords = mLastWordInPath.getConnectedWords();
            for (Word word : connectedWords){
                 if (!mWordsThatWasVisited.contains(word)
                         && !mWordsPath.contains(word)) {
                     double connectionLevel = mText.getConnectionLevelBetweenWords(word, mLastWordInPath);
                     if (connectionLevel > maxConnectionLevel)
                         nextWords.add(word);
                 }
            }

            return nextWords;
        }

        private boolean isTarget(){

            for (Word word : mNextWords){
                if (word.equals(mEndPoint)){
                    mWordsPath.add(word);
                    return true;
                }
            }

            return false;

        }

        @Override
        public void run() {

             if (isTarget()){
                 mThemes.add(mWordsPath);
             } else {

                 if (mWordsPath.size() > Settings.MAX_THEME_LENGTH)
                     return;

                 for (Word word : mNextWords){
                     List<Word> nextWords = new ArrayList(mWordsPath);
                     nextWords.add(word);
                     TextWalker textWalker = new TextWalker(nextWords);
                     mTasks.add(mExecutorService.submit(textWalker));
                 }
             }
        }
    }

}
