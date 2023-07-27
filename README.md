# Text Rendering using OpenGL

## Description

This project aims to render three-dimensional characters in Android using the OpenGL_Utilities library, which simplifies the process of programming with OpenGL on Android. While the library supports various shapes, it does not directly support rendering characters. Therefore, this work explores a method to obtain these characters by importing models.

The characters are created based on the best 3D file format, utilizing an appropriate modeling program. Subsequently, a parser is developed to read the data from these files. After reading the files, the characters are manipulated to create texts, incorporating techniques such as coloring and scaling.

The main objective of this project is to achieve the most efficient method for enhancing the performance of text rendering.

## Library Usage

This project utilizes a specific component from the [AndroidOpenGLLib](https://github.com/CarstenVogt/AndroidOpenGLLib) project, namely the "opengl_utilities" library, for rendering characters with OpenGL on Android. The AndroidOpenGLLib project is provided by Prof. Dr. Carsten Vogt, Technische Hochschule Köln, Fakultät für Informations-, Medien- und Elektrotechnik, Germany, under the GNU General Public License 3 (GPLv3). You can find the full license details [here](http://www.gnu.org/licenses/gpl-3.0.html).

---
