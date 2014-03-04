package ${groupId};

import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;

@State(Scope.Benchmark)
class MyBenchmark {

    @GenerateMicroBenchmark
    def pure() {
        // Put your benchmark code here
    }

}
