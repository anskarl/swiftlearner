package com.danylchuk.swiftlearner.data

import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

import com.danylchuk.swiftlearner.util

/**
  * The classic MNIST handwritten digits dataset.
  *
  * Ref.: http://yann.lecun.com/exdb/mnist/
  *       https://en.wikipedia.org/wiki/MNIST_database
  */
object Mnist {
  private val BinaryThreshold = 127

  /** Training images as flattened vectors (of concatenated lines) */
  def trainImages: Iterator[Vector[Float]] =
    trainImagesByteArray.iterator.map(_.toVector.map(util.byteToUnsignedInt(_).toFloat))
  def trainImagesDouble: Iterator[Vector[Double]] =
    trainImagesByteArray.iterator.map(_.toVector.map(util.byteToUnsignedInt(_).toDouble))
  def trainImagesBinary: Iterator[Vector[Int]] =
    trainImagesByteArray.iterator.map(_.toVector.map(util.byteToBinary(_, BinaryThreshold)))

  lazy val trainImagesByteArray: Array[Array[Byte]] = readImages(trainImagesFile)

  /** Labels corresponding to the training images */
  lazy val trainLabels: Stream[Int] = readLabels(trainLabelsFile)

  /** Test images as flattened vectors (of concatenated lines) */
  def testImages: Iterator[Vector[Float]] =
    testImagesByteArray.iterator.map(_.toVector).map(_.map(util.byteToUnsignedInt(_).toFloat))
  def testImagesDouble: Iterator[Vector[Double]] =
    testImagesByteArray.iterator.map(_.toVector).map(_.map(util.byteToUnsignedInt(_).toDouble))
  def testImagesBinary: Iterator[Vector[Int]] =
    testImagesByteArray.iterator.map(_.toVector.map(util.byteToBinary(_, BinaryThreshold)))

  lazy val testImagesByteArray: Array[Array[Byte]] = readImages(testImagesFile)

  /** Labels corresponding to the test images */
  lazy val testLabels: Stream[Int] = readLabels(testLabelsFile)

  def labeledTrainIterator = trainLabels.iterator zip trainImages
  def labeledTrainIteratorDouble = trainLabels.iterator zip trainImagesDouble
  def labeledTrainIteratorBinary = trainLabels.iterator zip trainImagesBinary
  def labeledTestIterator = testLabels.iterator zip testImages
  def labeledTestIteratorDouble = testLabels.iterator zip testImagesDouble
  def labeledTestIteratorBinary = testLabels.iterator zip testImagesBinary

  /** Convenience shortcut */
  def trainingAndTestDataFloat(nTrainPoints: Int = TrainSetSize)
  : (Seq[(Int, Vector[Float])], Seq[(Int, Vector[Float])]) = {
    val labeledTrainSet = labeledTrainIterator take nTrainPoints
    val labeledTestSet = labeledTestIterator take nTrainPoints
    (labeledTrainSet.toSeq, labeledTestSet.toSeq)
  }
  def trainingAndTestDataDouble(nTrainPoints: Int = TrainSetSize)
  : (Seq[(Int, Vector[Double])], Seq[(Int, Vector[Double])]) = {
    val labeledTrainSet = labeledTrainIteratorDouble take nTrainPoints
    val labeledTestSet = labeledTestIteratorDouble take nTrainPoints
    (labeledTrainSet.toSeq, labeledTestSet.toSeq)
  }
  def trainingAndTestDataBinary(nTrainPoints: Int = TrainSetSize)
  : (Seq[(Int, Vector[Int])], Seq[(Int, Vector[Int])]) = {
    val labeledTrainSet = labeledTrainIteratorBinary take nTrainPoints
    val labeledTestSet = labeledTestIteratorBinary take nTrainPoints
    (labeledTrainSet.toSeq, labeledTestSet.toSeq)
  }

  /** Recommended when the little extra cost is not critical. */
  def shuffledTrainingAndTestDataDouble(nTrainPoints: Int = TrainSetSize, randomSeed: Option[Long] = None)
  : (Seq[(Int, Vector[Double])], Seq[(Int, Vector[Double])]) = {
    val r = new scala.util.Random()
    randomSeed.foreach(r.setSeed)
    val (labeledTrainSet, labeledTestSet) = trainingAndTestDataDouble(nTrainPoints)
    (r.shuffle(labeledTrainSet), labeledTestSet)
  }
  def shuffledTrainingAndTestDataFloat(nTrainPoints: Int = TrainSetSize, randomSeed: Option[Long] = None)
  : (Seq[(Int, Vector[Float])], Seq[(Int, Vector[Float])]) = {
    val r = new scala.util.Random()
    randomSeed.foreach(r.setSeed)
    val (labeledTrainSet, labeledTestSet) = trainingAndTestDataFloat(nTrainPoints)
    (r.shuffle(labeledTrainSet), labeledTestSet)
  }
  def shuffledTrainingAndTestDataBinary(nTrainPoints: Int = TrainSetSize, randomSeed: Option[Long] = None)
  : (Seq[(Int, Vector[Int])], Seq[(Int, Vector[Int])]) = {
    val r = new scala.util.Random()
    randomSeed.foreach(r.setSeed)
    val (labeledTrainSet, labeledTestSet) = trainingAndTestDataBinary(nTrainPoints)
    (r.shuffle(labeledTrainSet), labeledTestSet)
  }

  val ImageWidth = 28
  val ImageHeight = 28
  val ImageSize = ImageWidth * ImageHeight
  val TrainSetSize = 60000
  val TestSetSize = 10000

  private val ImagesOffset = 16 // magic number + set size + image dimensions
  private val LabelsOffset = 8  // magic number + set size

  private lazy val trainImagesFile = getFileStream("train-images-idx3-ubyte")
  private lazy val trainLabelsFile = getFileStream("train-labels-idx1-ubyte")
  private lazy val testImagesFile = getFileStream("t10k-images-idx3-ubyte")
  private lazy val testLabelsFile = getFileStream("t10k-labels-idx1-ubyte")

  private def getFileStream(name: String): Iterator[Int] = {
    val inStream = new BufferedInputStream(new GZIPInputStream(
      this.getClass.getClassLoader.getResourceAsStream(s"mnist/$name.gz")))

    Iterator.continually(inStream.read()).takeWhile(-1 != _)
  }

  private def readLabels(bytes: Iterator[Int]): Stream[Int] =
    bytes.drop(LabelsOffset).toStream

  /** @return Images as flattened vectors (of concatenated lines) */
  private def readImages(bytes: Iterator[Int]): Array[Array[Byte]] = {
    bytes.drop(ImagesOffset)  // magic number + set size
      .map(_.toByte).grouped(ImageSize).map(_.toArray).toArray
  }
}