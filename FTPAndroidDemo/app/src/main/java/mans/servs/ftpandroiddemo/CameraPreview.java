package mans.servs.ftpandroiddemo;

/**
 * CameraPreview Class controla el SurfaceView para iniciar/cerrar la cámara
 * de acuerdo a las ocurrencias en la aplicación.
 *
 * Permite abrir la cámara y visualizar la observación en un frame dentro de la aplicación.
 */

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static android.content.ContentValues.TAG;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    // Variables necesarias para iniciar la cámara en el preview.
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;

    // Variables necesarias para iniciar la cámara en el preview.
    public CameraPreview(Context context, Camera camera) {
        super(context);

        // Crear una instancia de camara.
        mCamera = camera;

        // Instalar un Surface.Callback para notificar el momento en que
        // se crea y destruye la superficie.
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        // Obsoleto, pero requerido para versiones previas de Android.
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        // La superficie de vista se creó. Decir a la cáamra donde crear el preview.
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            //mCamera.setDisplayOrientation(90);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d(TAG, "Error al preparar la vista de camara: " + ex.getMessage());
        }
    } // END método surfaceCreated

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        if (mSurfaceHolder.getSurface() == null) {

            try {
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
                //mCamera.setDisplayOrientation(90);
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.d(TAG, "Error al preparar la vista de camara: " + ex.getMessage());
            }
        }

        // Detener el preview antes de realizar cambios.
        try {
            mCamera.stopPreview();
        } catch (Exception er) {
            // ignore: tried to stop a non-existent preview
        }

        // Reiniciar el preview con los nuevos ajustes.
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    } // END método surfaceChanged

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Encargarse de liberar la cámara.
        if (mCamera == null) {
            // Stop preview will stop updating the preview surface.
            //mCamera.setDisplayOrientation(0);
            mCamera.stopPreview();
            // Calls release() to release the camera for use by other application.
            mCamera.release();
        }
    } // END método surfaceDestroyed
} // END CameraPreview
