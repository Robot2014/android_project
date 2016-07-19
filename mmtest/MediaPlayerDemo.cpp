#include <media/mediaplayer.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <gui/SurfaceComposerClient.h>
#include <gui/Surface.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

using namespace android;

int main(int argc, char **argv)
{
    sp<ProcessState> ps(ProcessState::self());
    ps->startThreadPool();

    if(argc < 2)
    {
        return -1;
    }
    //open file:
    printf("[play url:%s]", argv[1]);
    int fd = open(argv[1], O_EXCL|O_RDONLY);
    if(fd < 0)
    {
        printf("[open fail:%s]", strerror(errno));
        return -1;
    }

    //init surface:
    sp<SurfaceComposerClient> mSurfaceComposerClient = new SurfaceComposerClient;
    sp<SurfaceControl> mSurfaceControl = mSurfaceComposerClient->createSurface(
                                         String8("SurfaceView"),
                                         1,
                                         1,
                                         PIXEL_FORMAT_RGBA_8888,
                                         0);
    SurfaceComposerClient::openGlobalTransaction();
    mSurfaceControl->setLayer(300000);
    mSurfaceControl->setSize(1920, 1080);
    mSurfaceControl->setPosition(0, 0);
    SurfaceComposerClient::closeGlobalTransaction();

    sp<Surface> mSurface = mSurfaceControl->getSurface();

    //init player:
    status_t ret;
    sp<MediaPlayer> mPlayer = new MediaPlayer;
    
    mPlayer->setDataSource(fd, 0, -1);
    mPlayer->setVideoSurfaceTexture(mSurface->getIGraphicBufferProducer());
    ret = mPlayer->prepare();
    if(ret != OK)
    {
        mPlayer.clear();
    }
    else
    {
        mPlayer->start();
    }
    while(1);
    mPlayer->stop();
    mPlayer.clear();
    close(fd);
}
