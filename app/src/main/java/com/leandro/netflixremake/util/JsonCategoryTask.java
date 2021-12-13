package com.leandro.netflixremake.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.leandro.netflixremake.model.Category;
import com.leandro.netflixremake.model.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class JsonCategoryTask extends AsyncTask<String,Void, List<Category>> {

    private ProgressDialog dialog;
    private final WeakReference<Context> context;
    private CategoryLoader categoryLoader;

    public JsonCategoryTask(Context context){
        this.context = new WeakReference<>(context);
    }

    public void setCategoryLoader(CategoryLoader categoryLoader){
        this.categoryLoader = categoryLoader;
    }

    //MAIN THREAD
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Context context = this.context.get();

        if (context != null)
        dialog = ProgressDialog.show(context,"Carregando","",true);
    }

    //Vai fazer a operacao em background THREAD
    @Override
    protected List<Category> doInBackground(String... strings) {
        String url = strings[0];
        try {
            URL requestUrl = new URL(url);

            HttpsURLConnection urlConnection = (HttpsURLConnection) requestUrl.openConnection();

            urlConnection.setReadTimeout(1000);
            urlConnection.setConnectTimeout(1000);

            int respondeCode = urlConnection.getResponseCode();
            if (respondeCode > 400){
                throw new IOException("Erro na comunicao do servidor");
            }

           InputStream inputStream = urlConnection.getInputStream();

            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String jsonAsString = toString(in);

            List<Category> categories = getCategories(new JSONObject(jsonAsString));
            in.close();

            return  categories;

        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Category> getCategories(JSONObject json) throws JSONException {
        List<Category> categories = new ArrayList<>();

        JSONArray categoryArray = json.getJSONArray("category");
        for (int i = 0; i < categoryArray.length(); i++) {
           JSONObject category = categoryArray.getJSONObject(i);
            String title = category.getString("title");
//---------------------------------------------------------------------------------------------
            List<Movie> movies = new ArrayList<>();
            JSONArray movieArray = category.getJSONArray("movie");
            for (int j = 0; j < movieArray.length() ; j++) {
                JSONObject movie = movieArray.getJSONObject(j);

                String coverUrl = movie.getString("cover_url");
                int id = movie.getInt("id");
                Movie movieObj = new Movie();
                movieObj.setCoverUrl(coverUrl);
                movieObj.setId(id);
                movies.add(movieObj);
            }
            Category categoryOBJ = new Category();
            categoryOBJ.setName(title);
            categoryOBJ.setMovies(movies);
            categories.add(categoryOBJ);
        }
     return categories;
    }

    private String toString(InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int lidos;
        while ((lidos = is.read(bytes)) > 0){
            baos.write(bytes , 0,lidos);
        }
        return new String(baos.toByteArray());
    }

    public interface CategoryLoader{
        void onResult(List<Category> categories);

    }

    //MAIN THREAD -- o qual vai estar ouvindo no final ou listener
    @Override
    protected void onPostExecute(List<Category> categories) {
        super.onPostExecute(categories);
        dialog.dismiss();
        if (categoryLoader != null){
            categoryLoader.onResult(categories);
        }
    }
}
