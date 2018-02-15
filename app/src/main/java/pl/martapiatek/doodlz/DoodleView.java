package pl.martapiatek.doodlz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.print.PrintHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marta on 23.09.2017.
 */

public class DoodleView extends View {

    // określenie czy użytkownik przesunął palec na odległość pozwalającą na ponowne rysowanie
    private static final float TOUCH_TOLERANCE = 10;
    private final Paint paintScreen; //wyświetlanie bitmapy na ekranie
    private final Paint paintLine; //rysowanie linii na bitmapie
    //mapy aktualnie rysowanych ścieżek i punktów na tych ścieżkach
    private final Map<Integer, Path> pathMap = new HashMap<>();
    private final Map<Integer, Point> previousPointMap = new HashMap<>();
    private Bitmap bitmap; // obszar rysunku
    private Canvas bitmapCanvas; // rysowanie na bitmapie

    public DoodleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paintScreen = new Paint();

        //początkowe parametry rysowanej linii
        paintLine = new Paint();
        paintLine.setAntiAlias(true); // wygładzanie krawędzi linii
        paintLine.setColor(Color.BLACK); // domuślny kolor - czarny
        paintLine.setStyle(Paint.Style.STROKE); // linia ciągła
        paintLine.setStrokeWidth(5); //domyślna szerokość linii
        paintLine.setStrokeCap(Paint.Cap.ROUND); // zaokrąglone końce linii
    }

    // tworzy obiekty Bitmap i Canvas na podstawie rozmiaru obiektu View
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE); //wymaż bitmapę (wypełnienie białym)
    }

    //wyczyść obraz
    public void clear() {
        pathMap.clear(); // usuń wszystkie ścieżki
        previousPointMap.clear(); // usuń wszystkie wcześniejsze punkty
        bitmap.eraseColor(Color.WHITE); // wyczyść bitmapę
        invalidate(); // odśwież ekran
    }

    public int getDrawingColor() {
        return paintLine.getColor();
    }

    //ustaw kolor malowanej linii
    public void setDrawingColor(int color) {
        paintLine.setColor(color);
    }

    public int getLineWidth() {
        return (int) paintLine.getStrokeWidth();
    }

    // ustaw szerokość linii
    public void setLineWidth(int width) {
        paintLine.setStrokeWidth(width);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //rysuj ekran tła
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        for (Integer key : pathMap.keySet())
            canvas.drawPath(pathMap.get(key), paintLine); // rysuj linię
    }

    //obsługiwanie dotyku


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked(); // rodzaj zdarzenia
        int actionIndex = event.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            touchStarted(event.getX(actionIndex), event.getY(actionIndex), event.getPointerId(actionIndex));
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            touchEnded(event.getPointerId(actionIndex));
        } else {
            touchMoved(event);
        }
        invalidate(); // odśwież rysunek
        return true;

    }

    //obsługa oderwania palca od ekranu
    private void touchEnded(int lineId) {

        Path path = pathMap.get(lineId);
        bitmapCanvas.drawPath(path, paintLine); // rysuj na bitmapCanvas
        path.reset();
    }

    //obsługa przeciągania palcem po ekranie
    private void touchMoved(MotionEvent event) {

        for (int i = 0; i < event.getPointerCount(); i++) {

            int pointerID = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerID);

            if (pathMap.containsKey(pointerID)) {

                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathMap.get(pointerID);
                Point point = previousPointMap.get(pointerID);

                //oblicz odległość pokonana przez wskaźnik od ostatniej aktualizacji jego położenia
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {

                    path.quadTo(point.x, point.y, (newX + point.x) / 2, (newY + point.y) / 2);

                    //zapisz nowe współrzędne
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    // obsługa dotknięcia ekranu
    private void touchStarted(float x, float y, int lineId) {

        Path path;
        Point point;

        if (pathMap.containsKey(lineId)) {
            path = pathMap.get(lineId);
            path.reset();
            point = previousPointMap.get(lineId);
        } else {
            path = new Path();
            pathMap.put(lineId, path);
            point = new Point();
            previousPointMap.put(lineId, point);
        }

        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;

    }

    //zapisz bieżący obraz w galerii
    public void saveImage() {

        //utwórz nazwę pliku
        final String name = "Doodlz" + System.currentTimeMillis() + ".jpg";

        //zapisz obraz w pamięci urządzenia
        String location = MediaStore.Images.Media.insertImage(
                getContext().getContentResolver(), bitmap, name, "Doodlz Drawing");

        if (location != null) {

            //wyświetl komunikat o zapisaniu obrazu
            Toast message = Toast.makeText(getContext(), R.string.message_saved, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
            message.show();
        } else {

            //wyświetl komunikat o błędzie zapisu
            Toast message = Toast.makeText(getContext(), R.string.message_error_saving, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
            message.show();
        }

    }

    //drukuj obraz
    public void printImage(){
        if(PrintHelper.systemSupportsPrint()){

            PrintHelper printHelper = new PrintHelper(getContext());

            //przeskaluj obraz tak, aby zmieścił się w obszarze wydruku
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            //wydrukuj
            printHelper.printBitmap("Doodlz Image", bitmap);
        }
        else {
            // komunikat o błędzie drukowania
            Toast message = Toast.makeText(getContext(), R.string.message_error_printing, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
            message.show();
        }
    }

}
