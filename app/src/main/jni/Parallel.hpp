#include <chrono>
#include <thread>

using namespace std;

class Parallel_process : public cv::ParallelLoopBody{

    private:
        jlongArray &matAddrs;
        int threadNo;
        ModelConfig &config;
        jsize size;
        jlong *value;

    public:
        Parallel_process(ModelConfig &config, jlongArray &addrs, int number, jsize s, jlong *v)
            :config(config), matAddrs(addrs), threadNo(number), size(s), value(v) {
        }

        virtual void operator()(const cv::Range& range) const{
            Detector detector;
            detector.setModelConfig(config);

            for (int i = range.start; i < size; i += threadNo){
                Mat &frame = *(Mat *) value[i];
                detector.process(frame);
            }
        }
};