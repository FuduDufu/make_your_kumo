# HEXA Controller App

## Төслийн танилцуулга

Энэхүү Android аппликейшн нь Hexapod роботыг гар утаснаас удирдах зориулалттайгаар боловсруулагдсан.

Роботын механик загвар болон үндсэн санааг дараах нээлттэй эхийн төслөөс авсан болно:

[Hexapod Robot Project](https://github.com/MakeYourPet/hexapod?utm_source=chatgpt.com)

Тус төслийн роботыг удирдах зориулалттай **Chica Server** програм нь нээлттэй эхээр түгээгдээгүй байсан тул энэхүү Android аппликейшнийг шинээр боловсруулсан.

Аппликейшн нь Servo2040 удирдлагын самбарт команд дамжуулж, Hexapod роботын хөдөлгөөн болон үйлдлүүдийг гар утаснаас удирдах боломжийг бүрдүүлдэг.

---

## Үндсэн боломжууд

* Hexapod роботтой холбогдох
* Servo2040 самбарт удирдлагын команд дамжуулах
* Роботын хөдөлгөөнийг удирдах
* Бодит цагийн удирдлага
* Android төхөөрөмжөөс робот удирдах интерфейс

---

## Ашигласан технологи

* Android Studio
* Java / Kotlin
* Android SDK
* Servo2040 Controller
* Hexapod Robotics

---

## Төслийн бүтэц

```text
app/
gradle/
gradlew
gradlew.bat
settings.gradle.kts
build.gradle.kts
```

---

## Хөгжүүлэлтийн орчин бэлтгэх

### Шаардлагатай програмууд

* Android Studio (хамгийн сүүлийн хувилбар санал болгож байна)
* Android SDK
* Git
* Android төхөөрөмж эсвэл Emulator

### Repository татах

```bash
git clone https://github.com/FuduDufu/HEXA_chan_app.git
cd HEXA_chan_app
```

### Android Studio дээр нээх

1. Android Studio ажиллуулах
2. **Open Project** сонгох
3. Төслийн хавтсыг сонгох
4. Gradle Sync дуусахыг хүлээх

### Build хийх

Android Studio дээр:

```text
Build → Make Project
```

эсвэл Terminal ашиглан:

```bash
./gradlew build
```

Windows орчинд:

```bash
gradlew.bat build
```

### Аппликейшн ажиллуулах

1. Android төхөөрөмж холбох эсвэл Emulator ажиллуулах
2. Android Studio дээрээс **Run** товч дарах
3. Апп суусны дараа роботтой холбогдон удирдах боломжтой

---

## Зохиогчийн тэмдэглэл

Энэхүү аппликейшн нь MakeYourPet-ийн Hexapod роботын төслийг гар утаснаас удирдах зорилгоор бие даан боловсруулагдсан болно.

Анхны төслийн Chica Server програмын эх код нээлттэй биш байсан тул роботтой холбогдон Servo2040 самбарт команд дамжуулах Android аппликейшнийг шинээр хөгжүүлсэн.
