***Writeup:***

**How does your system work? (if not addressed in comments in source)**

My build script generates a quick test file that has numbers new line separated. This is used to run tests during the build process. The run script just calls the standard mvn run script for Spring.

I read the file in using a random file reader class with a buffer for speed and every 50 lines I put the specific byte offset into a hashmap. I repeat this until the end keeping track of total line count. After being read I take the total line count, divide it by 10, and make that the cache size limit in Caffeine. This can likely be optimized.

On request of a line, the line is checked for range, 0-max lines, and if valid checked in cache. If not found in cache, a future is made that requests the line from disk and this future is send to cache for sharing with other web workers. The future uses the modulo of the requested line from the 50 line step and subtracts to get the nearest pointer to the line. This file pointer is read in and looped over and read until the desired line is found. Then the answer is returned. In the case of a cache it, the future result is returned.

I use 0 based indexing for lines as no specifics were requested and it makes more sense in the software development context.

**How will your system perform with a 1 GB file? a 10 GB file? a 100 GB file?**

Scaling should be reasonably linear. By not storing every single lines location and instead every 50 I greatly reduce the space required to store the line data with very little performance loss. This step amount can be adjusted up or down depending on file size to optimize memory usage. A 1GB file should take around 6 seconds to process, a 10GB about a minute, and a 100GB about 10 minutes. This isn't currently cached to disk but it could be in the future. I wanted to keep the project in scope and considering performance, it isn't a huge priority. The only adjustment that may be needed is turning the number of lines from an integer to a long. It is integers for now to save space since 2 billion lines is a lot of lines and that many longs would take up a lot more room in memory.

**How will your system perform with 100 users? 10000 users? 1000000 users?**

Disk reads will be the limitation and so caching will be key. An SSD can only do a few thousand operations per second. My retrievals from line requested to line retrieved are 20 microseconds when cached and vary from 50 to around 150 depending on line count, size per line, and various system IO oddities which may cause outliers with longer retrieval times. The current implementation should scale easily to a large user count IF provided with sufficient memory for caching and cache size is increased. Without this expect a high hundreds to low thousands of requests per second as a limit if line requests are pure random. This would allow for a reasonable experience with 10000 users and no issues at all with 100.  

I used JMeter to try to do some load testing. My results vary but have hit nearly 1000 responses per second at times using a random line value for each request. I did test arraylist versus hashmap for retrieval of lines with no consistently better option so I kept it as hashmap for better logic understanding.

**What documentation, websites, papers, etc did you consult in doing this assignment?**

Stackoverflow heavily. Some of the searches were for issues with setting things up properly or things not working as expected, shutdown hooks for instance. I did a lot of research on optimizing randomly accessing the file (Since it was noted as immutable) and that lead to the library that buffered random access. I also looked into optimizations for sizes of cache and line steps. Otherwise, a lot of standard questions that are simple enough to not need to memorize, syntax for creating new caches, return values and the like.

**What third-party libraries or other tools does the system use? How did you choose each library or framework you used?**

* The base project uses [Spring](https://spring.io/) Web along with Spring test for web services and tests respectively. I have used Spring before and knew how to use it so sticking with it was a good plan. 

* Specifically for file reading I use a library called [IO](https://mvnrepository.com/artifact/org.bitbucket.kienerj/io) which adds a buffered RandomFileReader which reads as fast as a buffered reader but with the bonus of random access. This is the key to having high performance without a proper database as the default Java implementation is reading by byte. 

* I use [Caffeine](https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine) for caching. Caffeine is a continuation of Guava Cache and I have used it before and enjoy the features such as asynchronous caching. It is very fast and due to having async support, means multiple requests in a short time should use the same file read to get the line. It also has good support for eviction and size limiting.

* For the purposes of performance testing, I added [Google Guava](https://mvnrepository.com/artifact/com.google.guava/guava) for a stopwatch.

Then it's just the associated dependencies with these libraries

**How long did you spend on this exercise? If you had unlimited more time to spend on this, how would you spend it and how would you prioritize each item?**

I spent an evening and a morning with thinking it over in between. If I had unlimited time, I would look into what bottlenecks the file read performance. Most likely it's system level related and can not be heavily improved. I attempted to use two file readers but it didn't seem to improve performance. Optimizing for cache hits may also be a good use of the time, figuring out a good balance between memory usage and hit rate. The biggest priority would be optimizing cache as disk speed itself is the main limitation for getting lines.

**If you were to critique your code, what would you have to say about it?**

Using steps for pointers to save on memory space without much performance loss can be confusing due to the math required. Line retrievals and such all require faith in a 3rd party library to get the right line in the file. It is a very fast solution considering the possible file sizes and limitation of no proper database or copying of the entirety of the data. Keeping the same file pointer open for the whole lifecycle is very smart from a speed perspective.

My experience with code reviews of Java Spring based code is very limited so I may not follow all the style guide perfectly or all Spring conventions.

I have to make the assumption that maven is installed for the build/run scripts. It was stated in instructions they can just invoke another tool though so I am hopeful. There's likely some optimizations to be made involving cache size based on file size 

Tests require a file passed in to run, build.sh does this automatically however. The method of passing the argument in could be possibly be improved to not be a system property.
