/*
 * Copyright (c) 2016 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CheeseActivity extends BaseSearchActivity {

    private Disposable mDisposable;

    // returns an observable that emits strings
    private Observable<String> createButtonClickObservable() {
        // create and return a new Observable
        // this defines what happens when it is subscribed to
        return Observable.create(emitter -> {
            // when the click event happens, call onNext on the emitter
            mSearchButton.setOnClickListener(view -> emitter.onNext(mQueryEditText.getText().toString()));

            // this prevents memory leak by removing the reference to the listener
            emitter.setCancellable(() -> mSearchButton.setOnClickListener(null));
        });
    }

    protected void onStart() {
        super.onStart();

        // creates the observable
        Observable<String> textChangeStream = createTextChangeObservable();
        Observable<String> buttonClickStream = createButtonClickObservable();

        // merge the two observables so you can react to both in the same way
        Observable<String> searchTextObservable = Observable.merge(textChangeStream, buttonClickStream);

        // subscribe to the observable with a simple consumer
        // accept() will be called when the observable emits an item
        mDisposable = searchTextObservable
                // start on UI thread for progress bar
                .observeOn(AndroidSchedulers.mainThread())
                // doOnNext() is called every time an item is emitted by an observable
                .doOnNext(s -> showProgressBar())
                // switch to IO thread
                .observeOn(Schedulers.io())
                // for each query, return a list of results
                .map(query -> mCheeseSearchEngine.search(query))
                // anything after this will be called on the UI thread
                .observeOn(AndroidSchedulers.mainThread())
                // since our showResult() call there updates the UI, it has to be on the UI thread
                .subscribe(result -> {
                    // perform search and show the results
                    hideProgressBar();
                    showResult(result);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }

    private Observable<String> createTextChangeObservable() {
        // create an observable
        Observable<String> textChangeObservable = Observable.create(emitter -> {
            // when an observer subscribes, make a TextWatcher
            final TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                // when the text changes, emit the new sequence
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    emitter.onNext(charSequence.toString());
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            };

            // add the watcher to the query text view
            mQueryEditText.addTextChangedListener(watcher);

            // make sure we don't keep any references
            emitter.setCancellable(() -> mQueryEditText.removeTextChangedListener(watcher));
        });
        // return the observable, with a filter such that queries of size == 1 don't get propagated further
        return textChangeObservable
                .filter(s -> s.length() >= 2)
                .debounce(500, TimeUnit.MILLISECONDS);
    }
}
