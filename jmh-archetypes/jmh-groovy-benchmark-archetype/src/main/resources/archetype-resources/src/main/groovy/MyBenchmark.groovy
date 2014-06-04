package ${groupId};

import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Benchmark;

@State(Scope.Benchmark)
class MyBenchmark {

    @Benchmark
    def testMethod() {
        // Put your benchmark code here
    }

}
