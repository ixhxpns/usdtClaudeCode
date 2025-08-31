#!/usr/bin/env node

/**
 * RSA密钥对生成工具
 * 用于生成RSA-2048位密钥对并配置到系统中
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

function generateRSAKeyPair() {
  console.log('🔐 正在生成RSA-2048密钥对...');
  
  const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: {
      type: 'spki',
      format: 'pem'
    },
    privateKeyEncoding: {
      type: 'pkcs8',
      format: 'pem'  
    }
  });

  // PEM格式密钥
  const publicKeyPEM = publicKey;
  const privateKeyPEM = privateKey;

  // 提取Base64部分（去除PEM头尾）
  const publicKeyBase64 = publicKey
    .replace(/-----BEGIN PUBLIC KEY-----\n/, '')
    .replace(/\n-----END PUBLIC KEY-----\n/, '')
    .replace(/\n/g, '');
    
  const privateKeyBase64 = privateKey
    .replace(/-----BEGIN PRIVATE KEY-----\n/, '')
    .replace(/\n-----END PRIVATE KEY-----\n/, '')
    .replace(/\n/g, '');

  return {
    publicKeyBase64,
    privateKeyBase64,
    publicKeyPEM,
    privateKeyPEM
  };
}

function writeEnvironmentFile(keys) {
  const envContent = `# RSA加密密钥对
# 由 generate-rsa-keys.js 自动生成于 ${new Date().toISOString()}

# RSA公钥 (Base64编码)
RSA_PUBLIC_KEY=${keys.publicKeyBase64}

# RSA私钥 (Base64编码) 
RSA_PRIVATE_KEY=${keys.privateKeyBase64}
`;

  const envFilePath = path.join(__dirname, '../backend/.env.rsa');
  fs.writeFileSync(envFilePath, envContent);
  console.log(`✅ RSA密钥已保存到: ${envFilePath}`);
}

function writeKeyFiles(keys) {
  const keysDir = path.join(__dirname, '../keys');
  
  if (!fs.existsSync(keysDir)) {
    fs.mkdirSync(keysDir, { recursive: true });
  }

  // 写入PEM格式密钥文件
  fs.writeFileSync(path.join(keysDir, 'public_key.pem'), keys.publicKeyPEM);
  fs.writeFileSync(path.join(keysDir, 'private_key.pem'), keys.privateKeyPEM);
  
  // 写入Base64格式密钥文件
  fs.writeFileSync(path.join(keysDir, 'public_key_base64.txt'), keys.publicKeyBase64);
  fs.writeFileSync(path.join(keysDir, 'private_key_base64.txt'), keys.privateKeyBase64);
  
  console.log(`✅ 密钥文件已保存到: ${keysDir}/`);
}

function testEncryption(keys) {
  console.log('🧪 测试RSA加密/解密...');
  
  const testMessage = 'Hello, USDT Trading Platform!';
  
  try {
    // 使用公钥加密
    const publicKeyObj = crypto.createPublicKey(keys.publicKeyPEM);
    const encrypted = crypto.publicEncrypt(publicKeyObj, Buffer.from(testMessage));
    const encryptedBase64 = encrypted.toString('base64');
    
    // 使用私钥解密
    const privateKeyObj = crypto.createPrivateKey(keys.privateKeyPEM);
    const decrypted = crypto.privateDecrypt(privateKeyObj, encrypted);
    const decryptedMessage = decrypted.toString();
    
    if (decryptedMessage === testMessage) {
      console.log('✅ RSA加密/解密测试成功');
      console.log(`   原文: ${testMessage}`);
      console.log(`   加密: ${encryptedBase64.substring(0, 50)}...`);
      console.log(`   解密: ${decryptedMessage}`);
      return true;
    } else {
      console.error('❌ RSA加密/解密测试失败');
      return false;
    }
  } catch (error) {
    console.error('❌ RSA测试异常:', error.message);
    return false;
  }
}

function updateApplicationYml(keys) {
  const appYmlPath = path.join(__dirname, '../backend/src/main/resources/application.yml');
  
  if (!fs.existsSync(appYmlPath)) {
    console.warn('⚠️ application.yml 文件不存在，跳过自动更新');
    return;
  }
  
  let content = fs.readFileSync(appYmlPath, 'utf8');
  
  // 更新RSA配置
  content = content.replace(
    /public-key:\s*\${RSA_PUBLIC_KEY:.*}/,
    `public-key: \${RSA_PUBLIC_KEY:${keys.publicKeyBase64}}`
  );
  
  content = content.replace(
    /private-key:\s*\${RSA_PRIVATE_KEY:.*}/,
    `private-key: \${RSA_PRIVATE_KEY:${keys.privateKeyBase64}}`
  );
  
  fs.writeFileSync(appYmlPath, content);
  console.log('✅ application.yml 已更新RSA密钥配置');
}

function generateDocumentation(keys) {
  const docContent = `# RSA密钥对配置文档

## 生成信息
- 生成时间: ${new Date().toISOString()}
- 密钥长度: 2048位
- 算法: RSA
- 编码格式: Base64 (配置文件) / PEM (密钥文件)

## 前端配置
前端会自动从以下接口获取公钥:
- \`/api/admin/auth/public-key\`
- \`/api/auth/public-key\`

## 后端配置
请确保以下环境变量已设置:
\`\`\`
RSA_PUBLIC_KEY=${keys.publicKeyBase64}
RSA_PRIVATE_KEY=${keys.privateKeyBase64}
\`\`\`

或者在application.yml中配置:
\`\`\`yaml
business:
  security:
    rsa:
      public-key: ${keys.publicKeyBase64}
      private-key: ${keys.privateKeyBase64}
\`\`\`

## 密钥轮换
为了安全起见，建议定期更新RSA密钥对:
1. 运行此脚本生成新的密钥对
2. 更新后端配置并重启服务
3. 前端会自动获取新的公钥

## 安全注意事项
1. 私钥必须严格保密，不得外泄
2. 公钥可以公开，用于数据加密
3. 密钥文件应具有适当的文件权限
4. 在生产环境中使用环境变量而非明文配置
`;
  
  fs.writeFileSync(path.join(__dirname, '../keys/README.md'), docContent);
  console.log('✅ 配置文档已生成');
}

function main() {
  console.log('🚀 RSA密钥对生成工具启动\n');
  
  try {
    // 生成密钥对
    const keys = generateRSAKeyPair();
    
    // 测试加密功能
    if (!testEncryption(keys)) {
      console.error('❌ 密钥对测试失败，终止操作');
      process.exit(1);
    }
    
    // 保存密钥文件
    writeKeyFiles(keys);
    
    // 生成环境变量文件
    writeEnvironmentFile(keys);
    
    // 更新应用配置
    updateApplicationYml(keys);
    
    // 生成文档
    generateDocumentation(keys);
    
    console.log('\n🎉 RSA密钥对生成完成!');
    console.log('\n📝 下一步操作:');
    console.log('1. 检查 backend/.env.rsa 文件');
    console.log('2. 将环境变量导入到系统中');
    console.log('3. 重启后端服务');
    console.log('4. 测试前端RSA加密功能');
    console.log('\n⚠️ 重要提醒: 请妥善保管私钥文件，避免泄露!');
    
  } catch (error) {
    console.error('❌ 生成RSA密钥对失败:', error.message);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = { generateRSAKeyPair, testEncryption };