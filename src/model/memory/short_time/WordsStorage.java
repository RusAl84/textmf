package model.memory.short_time;

import model.NoContentException;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class WordsStorage {

    private final String TAG = WordsStorage.class.getSimpleName();

    private final Set<Word> mWords = new CopyOnWriteArraySet<>();

    private long mMaxConnectionCount = WordsStorage.COUNT_NOT_SET;

    private int mMaxWordFormCount = WordsStorage.COUNT_NOT_SET;

    private static final int COUNT_NOT_SET = -1;

    private Word mMostPopularWord = null;

    public Word getMostPopularWord(){

        if (mMostPopularWord != null)
            return mMostPopularWord;

        return refreshMostPopularWord();
    }

    public Word refreshMostPopularWord(){
        long maxCount = 0;
        Word mostPopularWord = null;

        for (Word word : mWords){
            if (word.getCount() > maxCount){
                maxCount = word.getCount();
                mostPopularWord = word;
            }
        }

        mMostPopularWord = mostPopularWord;

        return mostPopularWord;
    }

    public int getMaximumWordFormsCount(){

        if (mMaxWordFormCount != WordsStorage.COUNT_NOT_SET)
            return mMaxWordFormCount;

        int maxCount = 0;
        for (Word word : mWords){
            if (word.getWordForms().size() > maxCount)
                maxCount = word.getWordForms().size();
        }

        mMaxWordFormCount = maxCount;
        return maxCount;

    }

    public boolean checkWordInStorage(String str){
        for (Word word : mWords){
            if (word.equals(str))
                return true;
        }

        return false;
    }

    /**
     * Creating Word object from String str
     * @param str
     *
     * EXAMPLE:
     * String str = "word";
     * Word newWord = wordsStorage.generateWordFromString(str);
     *
     * NOTE:
     * You can't call constructor of object Word, you need to call this method insted
     */
    public Word generateWordFromString(String str) throws NoContentException {

        if (str == null)
            throw new NoContentException(TAG + " Word generateWordFromString(String str): str == null || str.length() < Word.MAX_LENGTH_DIFFERENT_FOR_COMPARE");

        if (checkWordInStorage(str))
            return getWordFromString(str);
        Word newWord = new Word(str);
        addWord(newWord);

        return newWord;
    }

    public Word getWordFromString(String strWord) throws NoContentException{
        for (Word word : mWords){
             if (word.equals(strWord))
                 return word;
        }

        Word newWord = generateWordFromString(strWord);
        addWord(newWord);
        return newWord;
    }

    public long getWordsFormCount(){
        long count = 0;

        for (Word word : mWords)
            count += word.getCount();

        return count;
    }

    public void addWord(Word word){

        synchronized (mWords){
            mWords.add(word);
        }

    }

    public void clearStorage(){
        mWords.clear();
    }

    public long getWordsCount(){
        return mWords.size();
    }

    public Set<Word> getAllWord(){
        return mWords;
    }

    public long getMaxConnectionCount(){

       if (mMaxConnectionCount != COUNT_NOT_SET)
           return mMaxConnectionCount;

       for (Word word : mWords){
           if (word.getConnectedWords().size() > mMaxConnectionCount)
               mMaxConnectionCount = word.getConnectedWords().size();
       }

       return mMaxConnectionCount;

    }

}
