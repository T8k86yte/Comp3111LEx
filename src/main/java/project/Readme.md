To compile/ test the code now, first do

find . -name "*.class" -delete

to delete all previous class file,
then compile all file inside task2 by doing

javac -d . project/task2/model/*.java \
         project/task2/utils/*.java \
         project/task2/repo/*.java \
         project/task2/service/*.java \
         project/task2/ui/*.java \
         project/task2/AuthorPortalMain.java

(note that there might be file adding or removing, so it might vary)

then run the main program AuthorPortalMain.java by typing

java project.task2.AuthorPortalMain