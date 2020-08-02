#include <chrono>
#include <thread>

using namespace std;

class Parallel_process : public cv::ParallelLoopBody{

    private:
        vector<cv::Mat> &mats;
        int threadNo;
        ModelConfig &config;

    public:
        Parallel_process(ModelConfig &config, vector<cv::Mat> &video, int number):config(config), mats(video), threadNo(number){
        }

        virtual void operator()(const cv::Range& range) const{
            Detector detector;
            detector.setModelConfig(config);

            for (int i = range.start; i < mats.size(); i += threadNo){
                detector.process(mats[i]);
            }
        }
};