(ns webgl.core
  (:require [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.gl.webgl.constants :as glc]
            [thi.ng.geom.matrix :as mat]
            [thi.ng.geom.gl.camera :as cam]
            [thi.ng.geom.triangle :as tri]
            [thi.ng.geom.vector :as v]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.gl.webgl.animator :as anim]
            [thi.ng.geom.gl.shaders :as shaders]
            [thi.ng.geom.gl.shaders.basic :as basic]
            [thi.ng.geom.circle :as c]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.color.core :as col]
            [thi.ng.geom.aabb :as a]
            [thi.ng.geom.polygon :as poly]
            [thi.ng.geom.gl.glmesh :as glmesh]
            [thi.ng.math.core :as m]))

(enable-console-print!)

(defn ^:export demo
  []
  (enable-console-print!)
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        shader1   (shaders/make-shader-from-spec gl (basic/make-shader-spec-2d false))
        shader2   (shaders/make-shader-from-spec gl (basic/make-shader-spec-2d true))
        teeth     20
        model     (-> (poly/cog 0.5 teeth [0.9 1 1 0.9])
                      (gl/as-gl-buffer-spec {:normals false :fixed-color [1 0 0 1]})
                      (gl/make-buffers-in-spec gl glc/static-draw)
                      (assoc-in [:uniforms :proj] (gl/ortho view-rect))
                      (time))]
    (anim/animate
     (fn [t frame]
       (gl/set-viewport gl view-rect)
       (gl/clear-color-and-depth-buffer gl 1 0.98 0.95 1 1)
       ;; draw left polygon using color uniform (that's why we need to remove color attrib)
       (gl/draw-with-shader
        gl (-> model
               (assoc :shader  shader1)
               (update-in [:attribs] dissoc :color)
               (update-in [:uniforms] merge
                          {:model (-> mat/M44 (geom/translate (v/vec3 -0.48 0 0)) (geom/rotate t))
                           :color [0 1 1 1]})))
       ;; draw right polygon using color attribs
       (gl/draw-with-shader
        gl (-> model
               (assoc :shader shader2)
               (assoc-in [:uniforms :model]
                         (-> mat/M44 (geom/translate (v/vec3 0.48 0 0)) (geom/rotate (- (+ t (/ m/HALF_PI teeth))))))))
       true))))

(defn ^:export demo2
  []
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        model     (-> (a/aabb 0.8)
                      (geom/center)
                      (geom/as-mesh
                       {:mesh    (glmesh/indexed-gl-mesh 12 #{:col})
                        :attribs {:col (->> [[1 0 0] [0 1 0] [0 0 1] [0 1 1] [1 0 1] [1 1 0]]
                                            (map col/rgba)
                                            (attr/const-face-attribs))}})
                      (gl/as-gl-buffer-spec {})
                      (cam/apply (cam/perspective-camera {:aspect view-rect}))
                      (assoc :shader (shaders/make-shader-from-spec gl (basic/make-shader-spec-3d true)))
                      (gl/make-buffers-in-spec gl glc/static-draw))]
    (anim/animate
     (fn [t frame]
       (doto gl
         (gl/set-viewport view-rect)
         (gl/clear-color-and-depth-buffer col/WHITE 1)
         (gl/enable glc/depth-test)
         (gl/draw-with-shader
          (assoc-in model [:uniforms :model] (-> mat/M44 (geom/rotate-x t) (geom/rotate-y (* t 2))))))
       true))))

(defonce gl-ctx (gl/gl-context "main"))

(defonce camera (cam/perspective-camera {}))

(def shader-spec
  {:vs "void main() {
          gl_Position = proj * view * model * vec4(position, 1.0);
       }"
   :fs "void main() {
           gl_FragColor = vec4(0.3, 0.3, 1.0, 1.0);
       }"
   :uniforms {:view       :mat4
              :proj       :mat4
              :model      :mat4}
   :attribs  {:position   :vec3}})

(def triangle (geom/as-mesh (tri/triangle3 [[0.8 0 0] [-0.8 0 0] [0 0.8 0]])
                            {:mesh (glmesh/gl-mesh 3)}))

(defn combine-model-shader-and-camera
  [model shader-spec camera]
  (-> model
      (gl/as-gl-buffer-spec {})
      (assoc :shader (shaders/make-shader-from-spec gl-ctx shader-spec))
      (gl/make-buffers-in-spec gl-ctx glc/static-draw)
      (cam/apply camera)))

(defn spin
  [t]
  (geom/rotate-y mat/M44 (/ t 2)))

(defn draw-frame! [t]
  (doto gl-ctx
    (gl/clear-color-and-depth-buffer 0 0 0 1 1)
    (gl/draw-with-shader (assoc-in (combine-model-shader-and-camera triangle shader-spec camera)
                                   [:uniforms :model] (spin t)))))

;(defonce running
;  (demo))
  ;(anim/animate (fn [t] (draw-frame! t) true)))
(demo2)

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload [])
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

