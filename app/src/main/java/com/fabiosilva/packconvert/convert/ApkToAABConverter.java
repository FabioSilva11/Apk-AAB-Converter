package com.fabiosilva.packconvert.convert;

import android.content.Context;

import com.android.bundle.Config;
import com.android.tools.build.bundletool.commands.BuildBundleCommand;
import com.google.common.collect.ImmutableList;
import com.fabiosilva.packconvert.model.MetaData;
import com.fabiosilva.packconvert.signing.helper.SignHelper;
import com.fabiosilva.packconvert.signing.model.CertificateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import kotlin.io.FilesKt;

public class ApkToAABConverter extends FileConverter {

    private static final int BUFFER_SIZE = 1024 * 2;
    private final File AAPT2Binary;
    private final Path mProtoOutput;
    private final Path mBaseZip;
    private final Path mBundleConfigPath;
    private final Config.BundleConfig mBundleConfig;
    private final Path mNonSignedAAB;
    private final CertificateInfo mSigningCertInfo;
    private final List<MetaData> mMetaData;

    public ApkToAABConverter(Builder builder) {
        super(builder);
        AAPT2Binary = new File(getContext().getApplicationInfo().nativeLibraryDir, "libaapt2.so");
        String dirPath = getContext().getCacheDir().getAbsolutePath() + File.separator + "temp";
        Path mTempDir = Paths.get(dirPath);
        try {
            Files.createDirectories(mTempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mProtoOutput = mTempDir.resolve("proto.zip");
        mBaseZip = mTempDir.resolve("base.zip");
        mNonSignedAAB = mTempDir.resolve("non-signed.aab");
        mSigningCertInfo = builder.signingCertInfo;
        mBundleConfigPath = builder.bundleConfigPath;
        mBundleConfig = builder.bundleConfig;
        mMetaData = builder.metaData;
    }

    @Override
    public void start() throws Exception {
        createProtoFormatZip();
        createBaseZip();
        buildAab();
        sign();
    }

    private void createProtoFormatZip() throws Exception {
        if (!Files.exists(mProtoOutput)) Files.createFile(mProtoOutput);
        addLog("Criando arquivo zip formatado em proto");
        ProcessBuilder processBuilder = new ProcessBuilder();
        StringWriter stringWriter = new StringWriter();
        List<String> args = new ArrayList<>();
        args.add(AAPT2Binary.getAbsolutePath());
        args.add("convert");
        args.add(getInputPath().toAbsolutePath().toString());
        args.add("-o");
        args.add(mProtoOutput.toAbsolutePath().toString());
        args.add("--output-format");
        args.add("proto");
        processBuilder.command(args);
        Process process = processBuilder.start();
        Scanner scanner = new Scanner(process.getErrorStream());
        boolean hasError = false;
        while (scanner.hasNextLine()) {
            hasError = true;
            String log = scanner.nextLine();
            addLog(log);
            stringWriter.append(log);
            stringWriter.append(System.lineSeparator());
        }
        process.waitFor();
        if (hasError) throw new Exception(stringWriter.toString());
    }

    private void createBaseZip() throws IOException {
        addLog("Criando base.zip");
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(mProtoOutput.toFile()));
             ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(mBaseZip.toFile()));
             ZipFile inputZip = new ZipFile(mProtoOutput.toFile())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".dex") && entry.getName().startsWith("classes")) {
                    zipOutputStream.putNextEntry(
                            new ZipEntry("dex" + File.separator + entry.getName()));
                } else if (entry.getName().equals("AndroidManifest.xml")) {
                    zipOutputStream.putNextEntry(
                            new ZipEntry("manifest" + File.separator + entry.getName()));
                } else if (entry.getName().startsWith("res" + File.separator)) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
                } else if (entry.getName().startsWith("lib" + File.separator)) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
                } else if (entry.getName().equals("resources.pb")) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
                } else if (entry.getName().startsWith("assets" + File.separator)) {
                    zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));

                    // a pasta META-INF pode conter recursos não relacionados à assinatura,
                    // portanto, verificamos se a entrada não aponta para um arquivo de assinatura
                } else if (!entry.getName().endsWith(".RSA")
                        && !entry.getName().endsWith(".SF")
                        && !entry.getName().endsWith(".MF")) {
                    zipOutputStream.putNextEntry(new ZipEntry("root" + File.separator + entry.getName()));
                } else {
                    continue;
                }
                if (isVerbose()) addLog("adicionando " + entry.getName() + " para proto para base.zip");
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                try (InputStream is = inputZip.getInputStream(new ZipEntry(entry.getName()))) {
                    while ((len = is.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    private void buildAab() {
        addLog("Criando aab");
        BuildBundleCommand.Builder builder = BuildBundleCommand.builder()
                .setModulesPaths(ImmutableList.of(mBaseZip))
                .setOutputPath(mNonSignedAAB)
                .setOverwriteOutput(true);
        if (mBundleConfigPath != null) {
            builder.setBundleConfig(mBundleConfigPath);
        }
        if (mBundleConfig != null) {
            builder.setBundleConfig(mBundleConfig);
        }
        for (MetaData metaData : mMetaData) {
            builder.addMetadataFile(metaData.getDirectory(), metaData.getFileName(), metaData.getPath());
        }
        builder.build().execute();
        addLog("Conversão de Apk para AAB concluída com sucesso");
    }

    public void sign() {
        if (mSigningCertInfo != null) {
            addLog("Assinando AAB");
            SignHelper.INSTANCE.signWithFlinger(
                    mSigningCertInfo,
                    mNonSignedAAB.toFile(),
                    getOutputPath().toFile()
            );
        } else {
            addLog("Nenhuma configuração de assinatura fornecida, pulando a assinatura");
            FilesKt.copyTo(
                    mNonSignedAAB.toFile(),
                    getOutputPath().toFile(),
                    true,
                    8 * 1024
            );
        }
    }

    public static class Builder extends FileConverter.Builder<Builder> {
        private Path bundleConfigPath;
        private Config.BundleConfig bundleConfig;
        private final List<MetaData> metaData;
        private CertificateInfo signingCertInfo;

        public Builder(Context context, Path apkPath, Path outputPath) {
            super(context, apkPath, outputPath);
            metaData = new ArrayList<>();
        }

        public Builder setBundleConfig(Path configPath) {
            bundleConfig = null;
            this.bundleConfigPath = configPath;
            return this;
        }

        public Builder setBundleConfig(Config.BundleConfig bundleConfig) {
            bundleConfigPath = null;
            this.bundleConfig = bundleConfig;
            return this;
        }

        public Builder addMetaData(MetaData metaData) {
            this.metaData.add(metaData);
            return this;
        }

        public Builder setSigningCertInfo(CertificateInfo certInfo) {
            this.signingCertInfo = certInfo;
            return this;
        }

        @Override
        public ApkToAABConverter build() {
            return new ApkToAABConverter(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
