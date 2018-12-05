package jp.ac.asojuku.st.kadaiball

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.startActivity



class MainActivity : AppCompatActivity(), SensorEventListener,SurfaceHolder.Callback{

    private var surfaceWidth:Int = 0
    private var surfaceHeight:Int = 0;

    private val radius = 50.0f;
    private val coef = 1000.0f;

    //ボールの位置
    private var ballX:Float = 0f;
    private var ballY:Float = 0f;

    //加速度
    private var vx:Float = 0f;
    private var vy:Float = 0f;
    private var time:Long = 0L;

    //跳ねる量
    private val bound = 1.7f;
    private val cornerBound = 1.1f;

    //直前のボールの位置（壁の角に当たった時の動きを滑らかにする）
    private var prefX:Float = 0f;
    private var prefY:Float = 0f;

    //壁の数
    private var boxNum:Int = 0;

    //外壁の当たり判定（この時点では変数の値が0なので、後で再度初期化）
    val t0 = arrayOf(surfaceHeight-0f,surfaceWidth-0f,0f);
    val b0 = arrayOf(0f,surfaceWidth-0f,0f);
    val r0 = arrayOf(0f,0f,surfaceHeight-0f);
    val l0 = arrayOf(surfaceWidth-0f,0f,surfaceHeight-0f);

    //壁のそれぞれの面の範囲
    val lineTop = mutableListOf<Array<Float>>(t0);
    val lineBottom = mutableListOf<Array<Float>>(b0);
    val lineRight = mutableListOf<Array<Float>>(r0);
    val lineLeft = mutableListOf<Array<Float>>(l0);


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        val holder = surfaceView.holder;
        holder.addCallback(this);
        //リセットボタン(画面遷移で初期化)
        button.setOnClickListener{ startActivity<MainActivity>() }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event == null){ return; }
        //ゴールに着いたら、全部returnして止める。
        if((ballX+radius)>390 && (ballX-radius)<480 && (ballY-radius)<surfaceHeight && (ballY+radius)>surfaceHeight-85){
            return;
        }
//
        if(time==0L){time = System.currentTimeMillis();}
        if(event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] * -1;
            val y = event.values[1];
            var t = (System.currentTimeMillis() - time).toFloat();
            time = System.currentTimeMillis();
            t /= 1000.0f;

            val dx = (vx * t) + (x * t * t) / 2.0f;
            val dy = (vy * t) + (x * t * t) / 2.0f;
            prefX = ballX;
            prefY = ballY;

            ballX += (dx * coef);
            ballY += (dy * coef);
            vx += (x * t);
            vy += (y * t);

            //当たり判定(上手く使えばメソッド一つで良さそうだけど...配列作り直すの面倒...)
            if(vx>0){
                moveRight();
            }else{
                moveLeft();
            }
            if(vy>0){
                moveBottom();
            }else{
                moveTop();
            }

            drawCanvas();
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        surfaceWidth = width;
        surfaceHeight = height;

        ballX = (width /2).toFloat();
        ballY = (100).toFloat();
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        sensorManager.unregisterListener(this);
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        val sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(
                this,
                accSensor,
                SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun drawCanvas(){
        val canvas = surfaceView.holder.lockCanvas();
        //背景色
        canvas.drawColor(Color.GREEN)
        //動かすボール
        canvas.drawCircle(
                ballX,
                ballY,
                radius,
                Paint().apply{
                    color = Color.RED; }
        );
        //surfaceHeightとかはsurfaceを生成するまで初期化できないため、外壁の当たり判定はここで指定。
        t0[0] = surfaceHeight-0f;
        t0[1] = surfaceWidth-0f;

        b0[1] = surfaceWidth-0f;

        r0[2] = surfaceHeight-0f;

        l0[0] = surfaceWidth-0f;
        l0[2] = surfaceHeight-0f

        //壁の生成
        makeRect(50f,500f,800f,600f,canvas);
        makeRect(200f,740f,surfaceWidth-110f,840f,canvas);
        makeRect(200f,945f,300f,surfaceHeight-0f,canvas);
        makeRect(520f,1000f,620f,surfaceHeight-0f,canvas);
        makeRect(surfaceWidth-210f,840f,surfaceWidth-110f,surfaceHeight-140f,canvas);

        //壁の接続部分を結合
        lineRight[2][2] = surfaceHeight-140f;
        lineRight[5][1] = 740f;

        //ゴール
        canvas.drawCircle(410f,surfaceHeight-50f,50f, Paint().apply{ color = Color.BLUE; });

        surfaceView.holder.unlockCanvasAndPost(canvas);

    }
    //壁を作り、当たり判定を設定する
    private fun makeRect(left:Float,top:Float,right:Float,bottom:Float,canvas:Canvas){
        canvas.drawRect(left,top,right,bottom,Paint().apply { color=Color.BLACK });
        lineTop.add(arrayOf(top,right,left));
        lineBottom.add(arrayOf(bottom,right,left));
        lineRight.add(arrayOf(right,top,bottom));
        lineLeft.add(arrayOf(left,top,bottom));
        boxNum ++;
    }


    //右にボールが動いた時
    private fun moveRight(){
        //壁の数だけ判定
        for(i in 0..boxNum){
            //壁に当たったかどうかの判定
            if((ballX+radius) > lineLeft[i][0] && (ballX-radius) < lineLeft[i][0] && (ballY + radius) > lineLeft[i][1] && (ballY - radius) < lineLeft[i][2]){
                //壁の左上端の角に当たった時の処理
                if(ballY<lineLeft[i][1] && ballY+radius>lineLeft[i][1]) {
                    //丸みの分だけ跳ね返る処理を遅らせ、ボールの当たり判定を球状にする。
                    //壁の左の面の上端の角に当たると跳ねる
                    if((ballY - lineLeft[i][1]) * (ballY - lineLeft[i][1]) + (ballX - lineLeft[i][0]) * (ballX - lineLeft[i][0]) > radius * radius){
                        vy = vy * cornerBound;
                        return;
                    }
                }
                //壁の左下端の角に当たった時の処理
                if(ballY>lineLeft[i][2] && ballY-radius<lineLeft[i][2]) {
                    //丸みの分だけ跳ね返る処理を遅らせ、ボールの当たり判定を球状にする。
                    //壁の左の面の下端の角に当たると跳ねる
                    if ((ballY - lineLeft[i][2]) * (ballY - lineLeft[i][2]) + (ballX - lineLeft[i][0]) * (ballX - lineLeft[i][0]) > radius * radius) {
                        vy = vy * cornerBound;
                        return;
                    }
                }
                //上記の判定でreturnされなければ、壁に跳ねる処理を行う
                vx = -vx / bound;
//                ballX = (lineLeft[i][0] - radius);
                ballX = prefX;

            }
        }

    }
    //左にボールが動いた時
    private fun moveLeft(){
        for(i in 0..boxNum){
            if((ballX+radius) > lineRight[i][0] && (ballX-radius) < lineRight[i][0] && (ballY + radius) > lineRight[i][1] && (ballY - radius) < lineRight[i][2]) {
                if(ballY<lineRight[i][1] && ballY+radius>lineRight[i][1]) {

                    if((ballY - lineRight[i][1]) * (ballY - lineRight[i][1]) + (ballX - lineRight[i][0]) * (ballX - lineRight[i][0]) > radius * radius){
                        vy = vy * cornerBound;
                        return;
                    }
                }
                if(ballY>lineRight[i][2] && ballY-radius<lineRight[i][2]) {
                    if ((ballY - lineRight[i][2]) * (ballY - lineRight[i][2]) + (ballX - lineRight[i][0]) * (ballX - lineRight[i][0]) > radius * radius) {
                        vy = vy * cornerBound;
                        return;
                    }
                }

                vx = -vx / bound;
//                ballX = (lineRight[i][0] + radius);
                ballX = prefX;

            }
        }
    }
    //上にボールが動いた時
    private fun moveTop(){
        for(i in 0..boxNum){
            if((ballY - radius) < lineBottom[i][0] && (ballY + radius) > lineBottom[i][0] && (ballX-radius) < lineBottom[i][1] && (ballX+radius) > lineBottom[i][2]){
                if(ballX-lineBottom[i][1]<radius && ballX-lineBottom[i][1]>10) {
                    if((ballX - lineBottom[i][1]) * (ballX - lineBottom[i][1]) + (ballY - lineBottom[i][0]) * (ballY - lineBottom[i][0]) > radius * radius){
                            vx = vx / cornerBound;
                        return;
                    }
                }
                if(ballX-lineBottom[i][2]> -radius && ballX-lineBottom[i][2]<10) {
                    if ((ballX - lineBottom[i][2]) * (ballX - lineBottom[i][2]) + (ballY - lineBottom[i][0]) * (ballY - lineBottom[i][0]) > radius * radius) {
                        vx = vx * cornerBound;
                        return;
                    }
                }
                vy = -vy / bound;
//                ballY = (lineBottom[i][0] + radius);
                ballY = prefY;
            }
        }
    }
    //下にボールが動いた時
    private fun moveBottom(){
        for(i in 0..boxNum){
            if((ballY + radius) > lineTop[i][0] && (ballY - radius) < lineTop[i][0] && (ballX-radius) < lineTop[i][1] && (ballX+radius) > lineTop[i][2]){
                if(ballX<lineTop[i][1]+radius && ballX>lineTop[i][1]+10) {
                    if((ballX - lineTop[i][1]) * (ballX - lineTop[i][1]) + (ballY - lineTop[i][0]) * (ballY - lineTop[i][0]) > radius * radius){
                        vx = vx * cornerBound;
                        return;
                    }
                }
                if(ballX-lineTop[i][2]> -radius && ballX-lineTop[i][2]<10) {
                    if ((ballX - lineTop[i][2]) * (ballX - lineTop[i][2]) + (ballY - lineTop[i][0]) * (ballY - lineTop[i][0]) > radius * radius) {
                        vx = vx * cornerBound;
                        return;
                    }
                }
                vy = -vy / bound;
//                ballY = (lineTop[i][0] - radius);
                ballY = prefY;
            }
        }
    }

}
