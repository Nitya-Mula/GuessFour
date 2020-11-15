package edu.uic.cs478.f2020.nityamula.guessfour;

public class PlayerStatusBundle {
        String guessedNum;
        int correctPosCount;
        int wrongPosCount;
        int wrongNumber;

        public PlayerStatusBundle(String guessedNum, int correctPosCount, int wrongPosCount, int wrongNumber)
        {
            this.guessedNum = guessedNum;
            this.correctPosCount = correctPosCount;
            this.wrongPosCount = wrongPosCount;
            this.wrongNumber = wrongNumber;
        }
        public String getGuessedNum()
        {
            return guessedNum;
        }
        public int getCorrectPosCount()
        {
            return correctPosCount;
        }
        public int getWrongPosCount()
        {
            return wrongPosCount;
        }
        public int getWrongNumber()
        {
            return wrongNumber;
        }
}
