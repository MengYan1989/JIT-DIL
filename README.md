# JIT-Defect-Identification-and-Localization

Step 1: Detecting fix commits according to resolved defect issue ids. 

Step 2: Run RASZZ for data preparation.

Input: the fix commits in Step 1; 

Output: the label of each commit (bug-introducing or clean);
              the label of each added code line by each commit (buggy or clean);

Step 3: Run JIT-Features

Input: the labels in Step 2;

Output: the 14 change-level features integrated with labels in Step 2; 

Step 4: Run JIT-Defect-Identification

Input: the features with labels in Step 3; 

Output: the likelihood of being a bug-introducing commit for testing commits; 

Step 5: Run JIT-Defect-Localization

Input: (1) the added lines and their labels of each commit in Step 2; 

       (2) the predicted likelihood in Step 4; 
       
Output: the localization result according to the order sorted by line entropy for each identified-buggy or all-buggy commit; 

We are very grateful to Hellendoorn and Devanbu, Daniel Alencar da Costa et al. and Rosen et al. for sharing their scripts.
