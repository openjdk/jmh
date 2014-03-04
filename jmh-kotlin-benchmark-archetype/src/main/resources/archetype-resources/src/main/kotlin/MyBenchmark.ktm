package ${groupId}

import org.openjdk.jmh.annotations.GenerateMicroBenchmark

open class MyBenchmark {

    [GenerateMicroBenchmark]
    fun testMethod() {
        // Put your benchmark code here
    }

}