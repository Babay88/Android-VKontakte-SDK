package ru.babay.vk;

import android.util.Log;
import com.perm.kate.api.KException;
import com.perm.kate.api.Photo;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.util.List;

/**
 * Created by Babay on 01.02.14.
 */
public class Api extends com.perm.kate.api.Api {
    public Api(String access_token, String api_id) {
        super(access_token, api_id);
    }

    public Photo uploadWallPhoto(File file) throws KException, IOException, JSONException {
        String server = photosGetWallUploadServer(0L, 0L);
        JSONObject object = uploadFile(server, "photo", file);
        List<Photo> photos = saveWallPhoto(object.getString("server"), object.getString("photo"), object.getString("hash"), 0L, 0L);
        return photos.get(0);
    }


    private final static int MAX_TRIES = 3;

    protected JSONObject uploadFile(String url, String paramName, File file) throws IOException, MalformedURLException, JSONException, KException {
        Log.i(TAG, "url=" + url);
        String response = "";
        for (int i = 1; i <= MAX_TRIES; ++i) {
            try {
                if (i != 1)
                    Log.i(TAG, "try " + i);
                response = postFileRequestInternal(url, paramName, file);
                break;
            } catch (javax.net.ssl.SSLException ex) {
                processNetworkException(i, ex);
            } catch (SocketException ex) {
                processNetworkException(i, ex);
            }
        }
        Log.i(TAG, "response=" + response);
        JSONObject root = new JSONObject(response);
        checkError(root, url);
        //lastResult = root.getString("response");
        return root;
    }

    private String postFileRequestInternal(String url, String paramName, File file) throws IOException{
        HttpPost request = new HttpPost(URI.create(url));
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        byte[] bytes = readFullyAsByteArray(file);
        ByteArrayBody bab = new ByteArrayBody(bytes, file.getName());
        reqEntity.addPart(paramName, bab);
        request.setEntity(reqEntity);

        DefaultHttpClient client = new DefaultHttpClient(request.getParams());

        ResponseHandler responseHandler = new BasicResponseHandler();
        String response = (String) client.execute(request, responseHandler);

        return response;

    }

    public static byte[] readFullyAsByteArray(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
