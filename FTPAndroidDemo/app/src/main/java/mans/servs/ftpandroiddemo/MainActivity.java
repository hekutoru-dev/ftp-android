package mans.servs.ftpandroiddemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


//public class MainActivity extends AsyncTask<> implements OnClickListener {
public class MainActivity extends AppCompatActivity implements OnClickListener {

    // Global variables.
    private static final String TAG = "MainActivity";
    private static final String TEMP_FILENAME = "TAGtest.txt";
    private Context cntx = null;

    // Variables: camera instance validation.
    private Camera mCamera;

    // Handler: capture images activity cicle
    private Handler cameraHandler = new Handler();
    private static final int REQUEST_CAMERA_PERMISSIONS = 1;
    Spinner sItems;

    /* private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };*/

    private MyFTPClientFunctions ftpclient = null;

    // Configure FTP Connection elements.
    private Button btnLoginFtp, btnUploadFile, btnDisconnect, btnExit;
    private EditText edtHostName, edtUserName, edtPassword;
    private TextView statusText;

    // Show progress dialog connection and mobile file list results.
    private ProgressDialog pd;
    private String[] fileList;

    // Handle Upload files actions.
    private Handler handler = new Handler() {

        public void handleMessage(android.os.Message msg) {

            if (pd != null && pd.isShowing()) {
                pd.dismiss();
            }
            if (msg.what == 0) {
                getFTPFileList();
            } else if (msg.what == 1) {
                showCustomDialog(fileList);
            } else if (msg.what == 2) {
                Toast.makeText(MainActivity.this, "Uploaded Successfully!",
                        Toast.LENGTH_LONG).show();
            } else if (msg.what == 3) {
                Toast.makeText(MainActivity.this, "Disconnected Successfully!",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Unable to Perform Action!",
                        Toast.LENGTH_LONG).show();
            }
        }

    };

    /** ******************************************************************************************
     * METHOD: onCreate()
     * *******************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cntx = this.getBaseContext();

        edtHostName = findViewById(R.id.edtHostName);
        edtHostName.setText("192.168.10.100");
        edtUserName = findViewById(R.id.edtUserName);
        edtUserName.setText("hector");
        edtPassword = findViewById(R.id.edtPassword);
        edtPassword.setText("hector");

        btnLoginFtp = findViewById(R.id.btnLoginFtp);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnDisconnect = findViewById(R.id.btnDisconnectFtp);
        btnExit = findViewById(R.id.btnExit);
        statusText = findViewById(R.id.statusText);

        btnLoginFtp.setOnClickListener(this);
        btnUploadFile.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnExit.setOnClickListener(this);

        // ***** Crear Spinner con los datos de lapsos de tiempo ***********************************
        List<String> spinnerLapse = new ArrayList<String>();
        spinnerLapse.add("");
        spinnerLapse.add("5 segs");
        spinnerLapse.add("10 segs");
        spinnerLapse.add("15 segs");
        spinnerLapse.add("20 segs");
        spinnerLapse.add("1 min");
        spinnerLapse.add("2 mins");
        spinnerLapse.add("3 mins");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, spinnerLapse
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sItems = findViewById(R.id.lapse_spinner);
        sItems.setAdapter(adapter);
        // ---------- Spinner Lapsos de Tiempo -----------------------------------------------------

        // Verificar los permisos en tiempo de ejecución. ******************************************
        if (hasPermissions()) {
            // camera validation, open camera
            //validateCamera();
            Toast.makeText(MainActivity.this, "Ya se tienen permisos",
                    Toast.LENGTH_LONG).show();
        } else {
            requestPermissions();
        } // Verificar los permisos en tiempo de ejecución. ----------------------------------------


        createDummyFolder();
        ftpclient = new MyFTPClientFunctions();
        //Log.i("LIST", MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());

    } // END onCreate() method.


    /** ********************************************************************************************
     * Funciones de FTP
     ********************************************************************************************* */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLoginFtp:
                // Modificar para llamar a la cámara y su función de estár capturand imágenes y envíos.

                if (isOnline(MainActivity.this)) {
                    connectToFTPAddress();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Please check your internet connection!",
                            Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.btnUploadFile:
                pd = ProgressDialog.show(MainActivity.this, "", "Uploading...",
                        true, false);
                new Thread(new Runnable() {
                    public void run() {
                        boolean status;
                        status = ftpclient.ftpUpload(
                                Environment.getExternalStorageDirectory()
                                        + "/TAGFtp/" + TEMP_FILENAME,
                                TEMP_FILENAME, "/", cntx);
                        if (status) {
                            Log.d(TAG, "Upload success");
                            handler.sendEmptyMessage(2);
                        } else {
                            Log.d(TAG, "Upload failed");
                            handler.sendEmptyMessage(-1);
                        }
                    }
                }).start();
                break;

            case R.id.btnDisconnectFtp:
                pd = ProgressDialog.show(MainActivity.this, "", "Disconnecting...",
                        true, false);
                new Thread(new Runnable() {
                    public void run() {
                        ftpclient.ftpDisconnect();
                        handler.sendEmptyMessage(3);
                    }
                }).start();
                break;

            // finish App.
            case R.id.btnExit:
                stopCamera();
                this.finish();
                break;
        } // END switch().

    } // END onClick() method


    /** ********************************************************************************************
     * CREATE A TEMPORARY FOLDER FOR STORAGE OF IMAGES ------------------------------------------ OK
     ******************************************************************************************** */
    private void createDummyFolder() {

        File root = new File(Environment.getExternalStorageDirectory(),"AppCam");
        if (!root.exists()) {
            root.mkdirs();
        }
    } // END createDummyFolder().


    /** ********************************************************************************************
     * ESTABLISH CONNECTION TO ADDRESS VIA FTP -------------------------------------------------- OK
     ******************************************************************************************** */
    private void connectToFTPAddress() {

        final String host = edtHostName.getText().toString().trim();
        final String username = edtUserName.getText().toString().trim();
        final String password = edtPassword.getText().toString().trim();

        if (host.length() < 1) {
            Toast.makeText(MainActivity.this, "Please Enter Host Address!",
                    Toast.LENGTH_LONG).show();
        } else if (username.length() < 1) {
            Toast.makeText(MainActivity.this, "Please Enter User Name!",
                    Toast.LENGTH_LONG).show();
        } else if (password.length() < 1) {
            Toast.makeText(MainActivity.this, "Please Enter Password!",
                    Toast.LENGTH_LONG).show();
        } else {

            pd = ProgressDialog.show(MainActivity.this, "", "Connecting...",
                    true, false);

            new Thread(new Runnable() {
                public void run() {
                    boolean status = false;
                    status = ftpclient.ftpConnect(host, username, password, 21);
                    if (status) {
                        Log.d(TAG, "Connection Success");
                        handler.sendEmptyMessage(0);
                    } else {
                        Log.d(TAG, "Connection failed");
                        handler.sendEmptyMessage(-1);
                    }
                }
            }).start();
        } // END if-else clause.
    } // END connectToFTPAddress() method.


    /** ********************************************************************************************
     * GET A LIST OF FILES CONTENT IN MAIN FOLDER ----------------------------------------------- OK
     ******************************************************************************************** */
    private void getFTPFileList() {
        pd = ProgressDialog.show(MainActivity.this, "", "Getting Files...",
                true, false);

        new Thread(new Runnable() {

            @Override
            public void run() {
                fileList = ftpclient.ftpPrintFilesList("/");
                handler.sendEmptyMessage(1);
            }
        }).start();
    }

    /** ********************************************************************************************
     *  CHECK IF THERE IS A SERVICE CONNECTION -------------------------------------------------- OK
     ******************************************************************************************** */
    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    } // END isOnline() method.




    /** ********************************************************************************************
     *  SHOW A CUSTOM DIALOG -------------------------------------------------------------------- OK
     ******************************************************************************************** */
    private void showCustomDialog(String[] fileList) {
        // custom dialog
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.custom);
        dialog.setTitle("/ Directory File List");

        TextView tvHeading = dialog.findViewById(R.id.tvListHeading);
        tvHeading.setText(":: File List ::");

        if (fileList != null && fileList.length > 0) {
            ListView listView = dialog.findViewById(R.id.lstItemList);
            ArrayAdapter<String> fileListAdapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_list_item_1, fileList);
            listView.setAdapter(fileListAdapter);
        } else {
            tvHeading.setText(":: No Files ::");
        }

        Button dialogButton = dialog.findViewById(R.id.btnOK);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    } // END showCustomDialog() method.


    /** ********************************************************************************************
     *                      REQUESTING CAMERA AND STORAGE PERMISSIONS
     ******************************************************************************************** */
    // Implement onRequestPermisionsResult() method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Manejar la respuesta del usuario a la solicitud de permisos.
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(MainActivity.this, "Permiso negado.", Toast.LENGTH_SHORT).show();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.+
                    Toast.makeText(MainActivity.this, "Permiso concedido.", Toast.LENGTH_SHORT).show();
                }
                //return;
            }
        } // END switch()

    } // END onRequestPermisionResult() method.

    // Check if we have permissions to use the resources.
    private boolean hasPermissions() {
        // String array: Definición de permisos a solicitar.
        String[] permissions = new String[]{ Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        // Solicitar cada uno de los permisos.
        for (String perms:permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, perms)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, perms)) {

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{perms},
                            REQUEST_CAMERA_PERMISSIONS);
                }
            }
        }
        return true;

    } // END hasPermissions() method.

    // Request Permissions
    private void requestPermissions() {
        String[] permissions = new String[]{ Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions,REQUEST_CAMERA_PERMISSIONS);
        }
    } // END requestPermissions().

    // ======================== END REQUESTING REQUIRED PERMISSIONS ================================



    /** ********************************************************************************************
     *                              OPEN AND VALIDATING CAMERA
     ******************************************************************************************** */
    /** ******************************************************************************************
     * Obtiene una instancia de la cámara y la abre si existe y está disponible.
     * La cámara se abre en un FrameLayout por medio de una instancia de CameraPreview
     *          : Calls hasCameraHardware()
     * *******************************************************************************************/
    private void validateCamera() {
        if (hasCameraHardware(this)) {
            mCamera = getCameraInstance();
            Toast.makeText(MainActivity.this, "Abriendo Cámara", Toast.LENGTH_SHORT).show();
        } else
            return;

        // Crea el preview y lo inicia como contenido de la actividad.
        CameraPreview mCameraPreview = new CameraPreview(MainActivity.this, mCamera);
        FrameLayout frameLayout = findViewById(R.id.camera_preview);
        frameLayout.addView(mCameraPreview);
    } // END validateCamera() method. --------------------------------------------------------------


    /** ******************************************************************************************
     * Valida la existencia de la cámara en hardware.
     *
     * @param context, the camera context
     * @return : true if Camera hardware is available.
     *          : Calls getCameraInstance()
     * *******************************************************************************************/
    private boolean hasCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else
            Toast.makeText(MainActivity.this, "NO hay Cámara Disponible", Toast.LENGTH_SHORT).show();
        return false;
    } // END hasCameraHardware() method. -----------------------------------------------------------


    /** ******************************************************************************************
     * Obtener una instancia de Cámara.
     * Abre la cámara para iniciar la vista.
     * *******************************************************************************************/
    public static Camera getCameraInstance() {
        Camera cameraInstance = null;
        try {
            cameraInstance = Camera.open();
        } catch (Exception ex) {
            Log.d(TAG, "Fallo al intentar abrir la Camara: " + ex.getMessage());
        }

        return cameraInstance;
    } // END getCameraInstance() method. -----------------------------------------------------------


    // ============================== END OPEN & VALIDATING CAMERA =================================



    /** ******************************************************************************************
     *                  INTERFACE IMPLEMENTATION TO CAPTURE AND SEND IMAGES
     * ******************************************************************************************/
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            //Obtener el timestamp y guardarlo como un String
            //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String formatTimestamp = "yyyyMMdd'T'HHmmss";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatTimestamp, Locale.US);
            String formattedNow = simpleDateFormat.format(new Date());

            // Concatenar fecha y hora para el nombre del archivo a guardar.
            String filename = "F" + formattedNow + ".jpg";

            // Definir el directorio (en SD_CARD, externo) donde se guardaran las imágenes.
            /*File imageFile = new File(Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_PICTURES), filename);*/

            //File imageFile = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);

            File photo = new File(Environment.getExternalStorageDirectory() + "/AppCam/", filename);
            //File gpxfile = new File(photo, TEMP_FILENAME);

            // Utilizar el nombre y ruta dados para guardar la imagen.
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(photo.getPath()); // OR imageFile
                fileOutputStream.write(bytes);
                Toast.makeText(MainActivity.this, "Se ha guardado la imagen. ", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Enviar archivo por correo.
            //sendGMail(imageFile);
            // Enviar archivo por FTP.
            //connectToFTPAddress();
            Log.d("FILENAME", filename);
            sendImageFtp(filename);

            // Eliminar archivo después de enviarlo.
            //imageFile.delete();

            // Cada vez que se toma una imagen con la cámara es necesario
            // reiniciar el SurfaceView donde se visualiza la cámara.
            mCamera.startPreview();

        } // END onPictureTaken() method
    }; // END PictureCallback Interface


    /* *****************************************************************************************
     * ENVIAR IMAGEN CAPTURADA VIA FTP
     * ******************************************************************************************/
    private void sendImageFtp(final String filename) {
        pd = ProgressDialog.show(MainActivity.this, "", "Uploading...",
                true, false);
        Log.d("FILENAME B", Environment.getExternalStorageDirectory() + "/AppCam/" + filename);
        new Thread(new Runnable() {
            public void run() {
                boolean status; //= false;
                status = ftpclient.ftpUpload(Environment.getExternalStorageDirectory() + "/AppCam/" + filename,
                        filename, "/", cntx);
                if (status) {
                    Log.d(TAG, "Upload success");
                    handler.sendEmptyMessage(2);
                } else {
                    Log.d(TAG, "Upload failed");
                    handler.sendEmptyMessage(-1);
                }
            } // END sendImageFTP() method.
        }).start();
    }


    /** ******************************************************************************************
     * ENCENDIDO Y APAGADO DE LA CAMARA
     * INICIO Y FIN DE CAPTURA DE IMAGENES
     * *******************************************************************************************/
    private void controlCamera() {
        // Identificar el Switch que iniciará la captura de imágenes.
        final Switch mCameraSwitch = findViewById(R.id.switch_camera);

        mCameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            Runnable runnable;
            //String selectedLapse;
            long LAPSE_TIME = 15000;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { // Switch está en ON. Inicia captura de imágenes.

                    // Desactivar Spinner mientras está capturando imágenes.
                    sItems.setEnabled(false);

                    // Obtener el tiempo de lapso de captura del Spinner.
                    //String selectedLapse = sItems.getSelectedItem().toString();

                    // Pass selected time lapse to parameters. SWITCH CLAUSE
                    /* Pass selected time lapse to parameters.
                    // Convertir el tiempo obtenido de String a int y pasarlo al Runnable + Handler
                    switch (selectedLapse) {
                        case "5 segs":
                            LAPSE_TIME = 5000;
                            break;
                        case "10 segs":
                            LAPSE_TIME = 10000;
                            break;
                        case "15 segs":
                            LAPSE_TIME = 15000;
                            break;
                        case "20 segs":
                            LAPSE_TIME = 20000;
                            break;
                        case "1 min":
                            LAPSE_TIME = 60000;
                            break;
                        case "2 mins":
                            LAPSE_TIME = 120000;
                            break;
                        case "3 mins":
                            LAPSE_TIME = 180000;
                            break;
                        default:
                            LAPSE_TIME = 10000;
                            break;
                    }
                    */

                    // Iniciar el Runnable y repetirlo cada LAPSE_TIME seleccionado por el Spinner.
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            cameraHandler.postDelayed(this, LAPSE_TIME);
                            //Toast.makeText(MainActivity.this, "Se ha guardado la imagen. " + selectedLapse, Toast.LENGTH_SHORT).show();
                            mCamera.takePicture(null, null, mPictureCallback);
                        }
                    };
                    cameraHandler.postDelayed(runnable, LAPSE_TIME);
                } else { // Switch está en OFF
                    // Activar Spinner de LAPSE_TIME
                    sItems.setEnabled(true);
                    // Detener captura de imágenes.
                    cameraHandler.removeCallbacks(runnable);
                }
            } // END onCheckedChanged method()
        }); // END switch changed listener

    } // END controlCamera()

    // ============================== END CAPTURE & SEND IMAGES ====================================





    /** ********************************************************************************************
     *  CICLO DE ACTIVIDAD DE LA APLICACION.
     ******************************************************************************************** */
    private void stopCamera() {
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    protected void onPostResume() { super.onPostResume();
        controlCamera();}

    @Override
    protected void onResume() {
        super.onResume();
        validateCamera();
        controlCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

} // END MyFTPClientFunctions Class.
