package model.memory.short_time;

import model.NoContentException;

import java.util.ArrayList;
import java.util.List;

public class Sentanse {

    private static final String TAG = Sentanse.class.getSimpleName();

    private List<Word> mSentanseWords = null;

    private static final String SPLITTER = " ";

    public static Sentanse generateSentanse(String sentance, WordsStorage wordsStorage) throws NoContentException {
        String[] strWords = sentance.split(Sentanse.SPLITTER);

        List<Word> words = new ArrayList<Word>();

        for (int i = 0; i<strWords.length;i++){
            String strWord = strWords[i];

            if (i == 0)
                strWord = strWord.toLowerCase();

            if (strWord.length() == 1)
                continue;

            Word word = wordsStorage.generateWordFromString(strWord);
            if (word != null){
                words.add(word);
                word.increaseCount();
            }
        }

        if (words.size() == 0)
            throw new NoContentException(TAG+" generateSentanse(String sentance) words.size() == 0");

        return new Sentanse(words);
    }

    @Override
    public int hashCode(){
        long hashSum = 0;
        for (Word word : mSentanseWords)
            hashSum += word.hashCode();

        return (int) (hashSum / mSentanseWords.size());
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null)
            return false;

        if (obj instanceof Sentanse){
            Sentanse sentanse = (Sentanse) obj;
            if (sentanse.mSentanseWords.size() != mSentanseWords.size())
                return false;

            for (int i=0;i< mSentanseWords.size();i++)
                if (!mSentanseWords.get(i).equals(sentanse.mSentanseWords.get(i)))
                    return false;

            return true;
        }

        return false;

    }

    @Override
    public String toString(){
        String outStr = new String();
        for (Word word : mSentanseWords){
             outStr += (word + " ");
        }

        return outStr;
    }

    List<Word> getSentanseWords(){
        return mSentanseWords;
    }

    private Sentanse(List<Word> words) throws NoContentException{

        if (words == null || words.size() == 0)
            throw new NoContentException(TAG + " Sentanse(List<Word> words): words == null || words.size() == 0");

       mSentanseWords = words;
       for (int i = 0; i< mSentanseWords.size()-1;i++){
           Word firstWord = mSentanseWords.get(i);
           for (int j=i+1;j< mSentanseWords.size();j++){
               Word secondWord = mSentanseWords.get(j);
               Word.WordsConnection wordConnection = firstWord.addConnection(secondWord,j-i-1);
               secondWord.refreshConnection(firstWord, wordConnection);
           }
       }
    }

}
