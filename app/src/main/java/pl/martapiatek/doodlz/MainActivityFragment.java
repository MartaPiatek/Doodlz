package pl.martapiatek.doodlz;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    // okreslenia czy uzytkownik wstrzasnal urzadzeniem, aby skasowac rysunek
    private static final int ACCELEATION_TRESHOLD = 100000;
    //zmienna do identyfikacji żądania  użycia zewnetrznego magazynu
    private static final int SAVE_IMAGE_PERMISSION_REQUEST_CODE = 1;
    private DoodleView doodleView; // obsluga dotyku i rysowania
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private boolean dialogOnScreen = false;

    //interfejs obslugujacy zdarzenie przyspieszeniomierza
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            //upewnij sie, ze na ekranie nie sa wyswietlane inne okna dialogowe
            if (!dialogOnScreen) {

                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];


                // zapisz poprzednią wartość przyspieszenia
                lastAcceleration = currentAcceleration;

                //oblicz bieżącą wartość przyspieszenia
                currentAcceleration = x * x + y * y + z * z;

                //oblicz zmianę przyspieszenia
                acceleration = currentAcceleration * (currentAcceleration - lastAcceleration);

                if (acceleration > ACCELEATION_TRESHOLD)
                    confirmErase();
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        doodleView = (DoodleView) view.findViewById(R.id.doodleView);

        //inicjuj wartosc przyspieszenia
        acceleration = 0.00f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        return view;

    }

    //nasluchuj zdarzen czujnika
    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening(); //nasluchuj zdarzenia wstrzasu
    }

    private void enableAccelerometerListening() {
        //uzyskaj dostep do SensorManager
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        //zarejestruj obiekt nasluchujacy zdarzen przyspieszeniomierza
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    //zatrzymaj nasluchiwanie przyspieszeniomierza
    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening();
    }

    private void disableAccelerometerListening() {
        SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        sensorManager.unregisterListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

    }

    private void confirmErase() {
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        //  fragment.show(getFragmentManager(), "erase dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.doodle_fragment_menu, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.color:
                ColorDialogFragment colorDialog = new ColorDialogFragment();
                //colorDialog.show(getFragmentManager(), "color dialog");
                return true;

            case R.id.line_width:
                LineWidthDialogFragment widthDialog = new LineWidthDialogFragment();
                // widthDialog.show(getFragmentManager(), "line width dialog");
                return true;

            case R.id.delete_drawing:
                confirmErase();
                return true;

            case R.id.save:
                saveImage();
                return true;

            case R.id.print:
                doodleView.printImage();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void saveImage() {

        //sprawdz czy nadano uprawnienia potrzebne do zapisu
        if (getContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // tresc komunikatu
                builder.setMessage(R.string.permission_explanation);

                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //zażadaj potwierdzenia
                        requestPermissions(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, SAVE_IMAGE_PERMISSION_REQUEST_CODE);

                    }
                });
                builder.create().show();
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, SAVE_IMAGE_PERMISSION_REQUEST_CODE);
            }


        }
        doodleView.saveImage(); // zapisz obraz
    }


    //metoda wywoływana, gdy użytkownik odmówi lub udzieli uprawnień
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case SAVE_IMAGE_PERMISSION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    doodleView.saveImage();
                return;

        }
    }

    public DoodleView getDoodleView() {
        return doodleView;
    }

    public void setDialogOnScreen(boolean visible) {
        dialogOnScreen = visible;
    }
}
